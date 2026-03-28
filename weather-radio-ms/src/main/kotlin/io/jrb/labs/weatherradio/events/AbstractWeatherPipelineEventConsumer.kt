package io.jrb.labs.weatherradio.events

import io.jrb.labs.commons.eventbus.AbstractEventConsumer
import io.jrb.labs.commons.eventbus.EventBus
import io.jrb.labs.commons.eventbus.SystemEventBus
import kotlin.reflect.KClass

abstract class AbstractWeatherPipelineEventConsumer<E : Any>(
    kClass: KClass<E>,
    eventBus: EventBus,
    systemEventBus: SystemEventBus
) : AbstractEventConsumer<E>(
    kClass = kClass,
    eventBus = eventBus,
    systemEventBus = systemEventBus
)