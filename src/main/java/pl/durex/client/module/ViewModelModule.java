package pl.durex.client.module;

public final class ViewModelModule {

    private boolean enabled = false;

    public enum Hand { RIGHT, LEFT }
    private Hand activeHand = Hand.RIGHT;

    // Prawa ręka
    public float rightRotX = 0f, rightRotY = 0f, rightRotZ = 0f;
    public float rightPosX = 0f, rightPosY = 0f, rightPosZ = 0f;
    public float rightScale = 1f;

    // Lewa ręka
    public float leftRotX = 0f, leftRotY = 0f, leftRotZ = 0f;
    public float leftPosX = 0f, leftPosY = 0f, leftPosZ = 0f;
    public float leftScale = 1f;

    public static final float ROT_MIN = -180f, ROT_MAX = 180f;
    public static final float POS_MIN = -1f,   POS_MAX = 1f;
    public static final float SCALE_MIN = 0.1f, SCALE_MAX = 3f;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }

    public Hand getActiveHand() { return activeHand; }
    public void setActiveHand(Hand h) { activeHand = h; }
    public void toggleHand() { activeHand = activeHand == Hand.RIGHT ? Hand.LEFT : Hand.RIGHT; }

    // Gettery dla aktywnej ręki
    public float getRotX() { return activeHand == Hand.RIGHT ? rightRotX : leftRotX; }
    public float getRotY() { return activeHand == Hand.RIGHT ? rightRotY : leftRotY; }
    public float getRotZ() { return activeHand == Hand.RIGHT ? rightRotZ : leftRotZ; }
    public float getPosX() { return activeHand == Hand.RIGHT ? rightPosX : leftPosX; }
    public float getPosY() { return activeHand == Hand.RIGHT ? rightPosY : leftPosY; }
    public float getPosZ() { return activeHand == Hand.RIGHT ? rightPosZ : leftPosZ; }
    public float getScale() { return activeHand == Hand.RIGHT ? rightScale : leftScale; }

    public void setRotX(float v) { if (activeHand == Hand.RIGHT) rightRotX = v; else leftRotX = v; }
    public void setRotY(float v) { if (activeHand == Hand.RIGHT) rightRotY = v; else leftRotY = v; }
    public void setRotZ(float v) { if (activeHand == Hand.RIGHT) rightRotZ = v; else leftRotZ = v; }
    public void setPosX(float v) { if (activeHand == Hand.RIGHT) rightPosX = v; else leftPosX = v; }
    public void setPosY(float v) { if (activeHand == Hand.RIGHT) rightPosY = v; else leftPosY = v; }
    public void setPosZ(float v) { if (activeHand == Hand.RIGHT) rightPosZ = v; else leftPosZ = v; }
    public void setScale(float v) { if (activeHand == Hand.RIGHT) rightScale = v; else leftScale = v; }

    public void resetActive() {
        if (activeHand == Hand.RIGHT) {
            rightRotX = rightRotY = rightRotZ = 0f;
            rightPosX = rightPosY = rightPosZ = 0f;
            rightScale = 1f;
        } else {
            leftRotX = leftRotY = leftRotZ = 0f;
            leftPosX = leftPosY = leftPosZ = 0f;
            leftScale = 1f;
        }
    }

    public void resetAll() {
        rightRotX = rightRotY = rightRotZ = 0f;
        rightPosX = rightPosY = rightPosZ = 0f;
        rightScale = 1f;
        leftRotX = leftRotY = leftRotZ = 0f;
        leftPosX = leftPosY = leftPosZ = 0f;
        leftScale = 1f;
    }
}
