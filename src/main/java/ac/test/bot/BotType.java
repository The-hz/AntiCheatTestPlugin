package ac.test.bot;

public enum BotType {
    STATIONARY("stationary", "站桩假人", "无敌，无击退，不会死亡"),
    PVP("pvp", "PVP假人", "主动攻击范围内玩家，可死亡，有击退"),
    PASSIVE("passive", "被动假人", "受击不还手，可死亡，有击退"),
    COUNTER("counter", "反击假人", "被攻击时反击一下，无敌，无击退");

    private final String id;
    private final String displayName;
    private final String description;

    BotType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static BotType fromId(String id) {
        for (BotType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}