package tccrewplugin.notifiers.data;

import tccrewplugin.message.Field;
import tccrewplugin.util.Sanitizable;

import java.util.Collections;
import java.util.List;

public abstract class NotificationData implements Sanitizable {
    public List<Field> getFields() {
        return Collections.emptyList();
    }
}

