/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.eventbus.api;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraftforge.eventbus.ListenerList;

public class EventListenerHelper {
	private static final Map<Class<?>, ListenerList> listeners = new IdentityHashMap<>();
	private static ReadWriteLock lock = new ReentrantReadWriteLock(true);

	/**
	 * Returns a {@link ListenerList} object that contains all listeners
	 * that are registered to this event class.
	 *
	 * <p>This supports abstract classes that cannot be instantiated.
	 *
	 * <p>Note: this method is currently very slow.
	 */
	public static ListenerList getListenerList(Class<?> eventClass) {
		final Lock readLock = lock.readLock();
		// to read the listener list, let's take the read lock
		readLock.lock();
		ListenerList listenerList = listeners.get(eventClass);
		readLock.unlock();

		// if there's no entry, we'll end up here
		if (listenerList == null) {
			// Let's pre-compute our new listener list value. This will possibly call parents' listener list
			// evaluations. as such, we need to make sure we don't hold a lock when we do this, otherwise
			// we could conflict with the class init global lock that is implicitly present
			listenerList = computeListenerList(eventClass);
			// having computed a listener list, we'll grab the write lock.
			// We'll also take the read lock, so we're very clear we have _both_ locks here.
			final Lock writeLock = lock.writeLock();
			writeLock.lock();
			readLock.lock();
			// insert our computed value if no existing value is present
			listeners.putIfAbsent(eventClass, listenerList);
			// get whatever value got stored in the list
			listenerList = listeners.get(eventClass);
			// and unlock, and we're done
			readLock.unlock();
			writeLock.unlock();
		}

		return listenerList;
	}

	private static ListenerList computeListenerList(Class<?> eventClass) {
		if (eventClass == Event.class) {
			return new ListenerList();
		}

		ListenerList parentList = getListenerList(eventClass.getSuperclass());

		return new ListenerList(parentList);
	}
}
