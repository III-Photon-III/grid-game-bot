package game;

public final class Player {

    private Position position;
    private int levelsCompleted;

    public Player(Position startPosition) {
        this.position = startPosition;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getLevelsCompleted() {
        return levelsCompleted;
    }

    public void completeLevel() {
        levelsCompleted++;
    }

    public int getCurrentLevelNumber() {
        return levelsCompleted + 1;
    }
}