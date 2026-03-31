#!/usr/bin/env sh
  set -eu

  : "${RTL_FM_FREQUENCY:=162.400M}"
  : "${RTL_FM_MODE:=fm}"
  : "${RTL_FM_SAMPLE_RATE:=22050}"
  : "${RTL_FM_RESAMPLE_RATE:=22050}"
  : "${RTL_FM_DEVICE_INDEX:=0}"
  : "${AUDIO_TCP_PORT:=7355}"

  echo "Starting rtl_fm edge stream"
  echo "  device index: ${RTL_FM_DEVICE_INDEX}"
  echo "  frequency:    ${RTL_FM_FREQUENCY}"
  echo "  mode:         ${RTL_FM_MODE}"
  echo "  sample rate:  ${RTL_FM_SAMPLE_RATE}"
  echo "  resample:     ${RTL_FM_RESAMPLE_RATE}"
  echo "  tcp port:     ${AUDIO_TCP_PORT}"

  exec rtl_fm \
    -d "${RTL_FM_DEVICE_INDEX}" \
    -f "${RTL_FM_FREQUENCY}" \
    -M "${RTL_FM_MODE}" \
    -s "${RTL_FM_SAMPLE_RATE}" \
    -r "${RTL_FM_RESAMPLE_RATE}" \
    - \
    | socat - "TCP-LISTEN:${AUDIO_TCP_PORT},reuseaddr,fork"