package tccrewplugin.util;

import net.runelite.client.callback.ClientThread;

import java.util.function.BooleanSupplier;

public class BlockingClientThread extends ClientThread {
	@Override public void invoke(Runnable r) { r.run(); }
	@Override public void invoke(BooleanSupplier r) { while (!r.getAsBoolean()) { } }
	@Override public void invokeLater(Runnable r) { r.run(); }
	@Override public void invokeLater(BooleanSupplier r) { while (!r.getAsBoolean()) { } }
	@Override public void invokeAtTickEnd(Runnable r) { r.run(); }
}

