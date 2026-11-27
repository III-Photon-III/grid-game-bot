package vk;

/** Интерфейс отправки сообщений VK-боту. */
public interface VkApi {

    void sendMessage(long peerId, String text);

    /**
     * Отправляет сообщение со встроенной клавиатурой (кнопками).
     *
     * @param peerId   получатель
     * @param text     текст сообщения
     * @param keyboard JSON-клавиатура в формате VK Bots API
     */
    void sendMessageWithKeyboard(long peerId, String text, String keyboard);
}
