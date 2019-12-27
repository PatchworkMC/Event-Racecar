package net.minecraftforge.eventbus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraftforge.eventbus.api.EventRegistrarRegistry;
import net.minecraftforge.eventbus.api.IEventBus;

public final class EventRegistrarRegistryImpl implements EventRegistrarRegistry {
	private Map<Class, Consumer<IEventBus>> staticRegistrars;
	private Map<Class, BiConsumer<Object, IEventBus>> instanceRegistrars;

	public EventRegistrarRegistryImpl() {
		staticRegistrars = new HashMap<>();
		instanceRegistrars = new HashMap<>();
	}

	@Override
	public void registerStatic(Class<?> clazz, Consumer<IEventBus> registrar) {
		System.out.println("Static registrar registration: " + clazz + " " + registrar);

		staticRegistrars.put(clazz, registrar);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void registerInstance(Class<T> clazz, BiConsumer<T, IEventBus> registrar) {
		instanceRegistrars.put(clazz, (BiConsumer<Object, IEventBus>) registrar);
	}

	@Override
	public Consumer<IEventBus> getStaticRegistrar(Class clazz) {
		return staticRegistrars.get(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	public BiConsumer<Object, IEventBus> getInstanceRegistrar(Class clazz) {
		return instanceRegistrars.get(clazz);
	}
}
