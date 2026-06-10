package tccrewplugin;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Dimension;
import java.awt.Graphics2D;

@Singleton
public class ClanEventOverlay extends Overlay {

    private final ClanEventManager clanEventManager;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public ClanEventOverlay(ClanEventManager clanEventManager) {
        this.clanEventManager = clanEventManager;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    @Override
    public @Nullable Dimension render(Graphics2D graphics) {
        String text = clanEventManager.getDisplayText();
        if (text == null) {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(LineComponent.builder().left(text).build());
        return panelComponent.render(graphics);
    }
}

