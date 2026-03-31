# weather-radio-edge

`weather-radio-edge` is a small edge-node deployment for streaming live NOAA Weather Radio audio from an RTL-SDR receiver to the main `weather-radio-ms` application.

This deployment is intended to run on a lightweight Linux SBC such as a Libre Computer La Frite, while the main Spring Boot application runs elsewhere, such as on a Beelink SER3 or within K3S.

The edge node is intentionally minimal. Its responsibilities are:

- access the USB RTL-SDR device
- run `rtl_fm`
- demodulate NOAA Weather Radio audio
- stream raw PCM audio over TCP
- optionally self-update via Watchtower

The edge node does **not** run the full weather-radio application stack.

## Architecture

```text
RTL-SDR USB dongle
  -> rtl_fm_edge container on edge SBC
  -> TCP PCM audio stream
  -> weather-radio-ms application
  -> segmentation / SAME decoding / fusion / reporting / REST
```

This design keeps USB and SDR concerns local to the edge device while allowing the heavier JVM-based application to run on a more capable machine.

## Directory Layout

```text
weather-radio-edge/
├── docker-compose.yml
├── .env
├── docker/
│   └── rtl-fm-edge/
│       ├── Dockerfile
│       └── entrypoint.sh
└── logs/
```

## Requirements

### Hardware

- Linux SBC or small host
- supported RTL-SDR USB receiver
    - Nooelec RTL-SDR v5
    - RTL-SDR Blog V4
- suitable antenna for NOAA Weather Radio reception

### Software

- Linux with Docker and Docker Compose
- internet access for initial image build
- host configured so DVB kernel drivers do not claim the SDR

## Supported Use Case

This edge stack is designed for:

- one SDR device
- one NOAA Weather Radio frequency
- one outbound TCP audio stream

It is not intended to be a multi-radio or multi-station SDR platform.

## NOAA Weather Radio Frequency

Set the desired NOAA Weather Radio frequency using environment variables.

Example:

- `162.400M` for Burlington, VT (`KIG60`)

Adjust this based on your local transmitter.

## Host Preparation

### 1. Blacklist DVB kernel modules

The host operating system must not auto-claim the RTL-SDR as a DVB device.

Create a file such as:

`/etc/modprobe.d/rtl-sdr-blacklist.conf`

with contents:

```conf
blacklist dvb_usb_rtl28xxu
blacklist rtl2832
blacklist rtl2830
```

Then reboot the system.

### 2. Verify the SDR is visible

After reboot, confirm the device is visible:

```bash
lsusb
```

You should see your RTL-SDR listed.

## Configuration

### `.env`

Create a `.env` file in this directory:

```dotenv
RTL_FM_DEVICE_INDEX=0
RTL_FM_FREQUENCY=162.400M
RTL_FM_MODE=fm
RTL_FM_SAMPLE_RATE=22050
RTL_FM_RESAMPLE_RATE=22050
AUDIO_TCP_PORT=7355
```

### Variable Notes

- `RTL_FM_DEVICE_INDEX`
    - RTL-SDR device index passed to `rtl_fm`
    - use `0` when only one dongle is attached

- `RTL_FM_FREQUENCY`
    - NOAA Weather Radio frequency, such as `162.400M`

- `RTL_FM_MODE`
    - modulation mode passed to `rtl_fm`
    - use `fm`

- `RTL_FM_SAMPLE_RATE`
    - audio sample rate output by `rtl_fm`

- `RTL_FM_RESAMPLE_RATE`
    - resampled output rate
    - should match the application-side expectation

- `AUDIO_TCP_PORT`
    - TCP port exposed by the edge node for raw audio streaming

## Docker Compose

The `docker-compose.yml` file runs:

- `rtl_fm_edge`
    - captures SDR audio and streams it over TCP
- `watchtower`
    - optional automatic updates for the edge container

## Build and Run

From the `weather-radio-edge` directory:

```bash
docker compose up --build -d
```

To view logs:

```bash
docker compose logs -f rtl_fm_edge
```

To stop the stack:

```bash
docker compose down
```

## Verifying the Stream

### Check container health

```bash
docker compose ps
```

### Check logs

```bash
docker compose logs -f rtl_fm_edge
```

You should see the startup log lines from `entrypoint.sh`, followed by `rtl_fm` output.

### Check that the TCP port is listening

On the edge node:

```bash
ss -ltnp | grep 7355
```

Or from another machine:

```bash
nc -vz <edge-hostname-or-ip> 7355
```

## Connecting from `weather-radio-ms`

Configure the main application to use TCP ingestion mode:

```yaml
application:
  ingestion:
    mode: tcp
    tcp-host: wxedge01
    tcp-port: 7355
    sample-rate-hz: 22050
    frequency-mhz: 162.400
```

Replace `wxedge01` with the hostname or IP address of this edge node.

## Receiver Swapping

This design supports swapping between supported RTL-SDR receivers without changing the main weather-radio application.

Typical workflow:

1. connect the new SDR
2. determine its device index
3. update `.env`
4. restart the edge stack

If multiple SDRs are attached later, you may want to move from index-based selection to a more stable serial-based approach.

## Watchtower

Watchtower is included for convenience so the edge image can be updated automatically.

Current behavior:

- checks periodically for a newer image
- updates the `rtl_fm_edge` container
- removes old images after update

If you do not want automatic updates, remove or disable the `watchtower` service in `docker-compose.yml`.

## Operational Notes

### Resource expectations

This stack is intentionally lightweight and should be appropriate for a small SBC such as La Frite, provided the box is dedicated to this purpose.

### Recommended responsibilities for this node

Keep this node limited to:

- SDR USB access
- `rtl_fm`
- TCP audio forwarding
- optional Watchtower

Do not plan to run the full Spring Boot application, SAME decoding, or transcription here unless you have already validated performance.

## Troubleshooting

### `rtl_fm` cannot open the SDR

Possible causes:

- DVB kernel driver is still attached
- incorrect USB device mapping
- SDR not connected
- wrong device index

Try:

```bash
lsusb
docker compose logs -f rtl_fm_edge
```

### Docker container starts but no audio is received

Check:

- correct NOAA frequency
- antenna placement
- TCP port connectivity
- main application `tcp-host` and `tcp-port`

### Build fails with missing `entrypoint.sh`

Make sure:

- `docker/rtl-fm-edge/entrypoint.sh` exists
- the Dockerfile `COPY` path matches the Compose build context

### TCP port is open but application sees no useful audio

Check:

- sample rates match between edge and app
- tuned frequency is correct
- NOAA transmitter is in range
- antenna is appropriate for VHF weather radio

## Future Enhancements

Possible future improvements for this edge node include:

- serial-number-based SDR selection
- TLS or authenticated streaming
- lightweight health endpoint
- reconnect-aware upstream client behavior
- systemd wrapper for Docker Compose
- prebuilt image publishing

## Development Notes

This edge deployment pairs naturally with a local IntelliJ workflow:

- `rtl_fm_edge` runs in Docker on the edge node
- `weather-radio-ms` runs in IntelliJ or on a more capable host
- the app connects over TCP using ingestion mode `tcp`

This is the preferred development path when direct USB access from the IDE host is inconvenient.

## License

This project is released under the MIT License.