package game;

public final class VkApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int errorCode;

    public VkApiException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
