package net.minecraftforge.eventbus;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public enum EventBusEngine {
	INSTANCE;

	private final EventSubclassTransformer eventTransformer;

	EventBusEngine() {
		LogManager.getLogger().debug(LogMarkers.EVENTBUS, "Loading EventBus transformer");
		this.eventTransformer = new EventSubclassTransformer();
	}

	public boolean processClass(final ClassNode classNode, final Type classType) {
		return eventTransformer.transform(classNode, classType).isPresent();
	}

	public boolean handlesClass(final Type classType) {
		final String name = classType.getClassName();
		return !(name.equals("net.minecraftforge.eventbus.api.Event") ||
				name.startsWith("net.minecraft.") ||
				name.indexOf('.') == -1);
	}
}
