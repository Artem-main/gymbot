package MyBot.gymbot.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

public class WeightInputService {
    private final Map<String, Boolean> waitingForNumbers = new HashMap<>();
    private final String EXERCISE_NAME = "Жим гантелей стоя - 4х10";

    // Активация режима ожидания числа
    public void startWaitingForNumber(String chatId) {
            waitingForNumbers.put(chatId, true);
    }

    // Проверка на ожидание числа
    public boolean isWaitingForNumber(String chatId) {
           return waitingForNumbers.getOrDefault(chatId, false);
    }

    // Обработка введенного числа
    public void processNumberInput(String chatId, String text, Executor executor) {
        try {
            int number = Integer.parseInt(text);
            waitingForNumbers.remove(chatId);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(EXERCISE_NAME + ": " + number + " кг");

            try {
                executor.execute(sendMessage);
            } catch (TelegramApiException e) {
                handleSendError(chatId, executor);
            }
        } catch (NumberFormatException e) {
            handleInvalidInput(chatId, executor);
        }
    }

    // Обработка ошибки отправки
    private void handleSendError(String chatId, Executor executor) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Ошибка при отправке результата");

        try {
            executor.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.err.println("Критическая ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    // Обработка неверного ввода
    private void handleInvalidInput(String chatId, Executor executor) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Ошибка! Введите числовое значение веса");

        try {
            executor.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке сообщения об ошибке: " + e.getMessage());
        }
    }

    // Интерфейс для выполнения отправки сообщений
    public interface Executor {
        void execute(SendMessage message) throws TelegramApiException;
    }
}
