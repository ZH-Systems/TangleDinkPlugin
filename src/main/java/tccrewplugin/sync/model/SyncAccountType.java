package tccrewplugin.sync.model;

import tccrewplugin.domain.AccountType;

public enum SyncAccountType
{
    STANDARD,
    IRON,
    HARDCORE_IRON,
    ULTIMATE_IRON,
    UNRANKED_IRON,
    GROUP_IRON,
    HARDCORE_GROUP_IRON,
    PLAYER_MODERATOR,
    JAGEX_MODERATOR;

    public static SyncAccountType from(AccountType accountType)
    {
        if (accountType == null)
        {
            return STANDARD;
        }

        switch (accountType)
        {
            case IRONMAN:
                return IRON;
            case HARDCORE_IRONMAN:
                return HARDCORE_IRON;
            case ULTIMATE_IRONMAN:
                return ULTIMATE_IRON;
            case UNRANKED_GROUP_IRONMAN:
                return UNRANKED_IRON;
            case GROUP_IRONMAN:
                return GROUP_IRON;
            case HARDCORE_GROUP_IRONMAN:
                return HARDCORE_GROUP_IRON;
            default:
                return STANDARD;
        }
    }
}
