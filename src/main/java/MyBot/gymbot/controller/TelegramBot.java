package MyBot.gymbot.controller;

import MyBot.gymbot.config.properties.BotProperties;
import MyBot.gymbot.keyBoard.MassExercisesMenuKeyboard;
import MyBot.gymbot.keyBoard.MainMenu;
import MyBot.gymbot.service.BotCommand;
import MyBot.gymbot.service.ReadExercises;
import MyBot.gymbot.service.WeightInputService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static MyBot.gymbot.service.BotCommand.FINISH_EXERCISE;


@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;
    private final MainMenu mainMenu = new MainMenu();
    private final MassExercisesMenuKeyboard massExercisesMenuKeyboard = new MassExercisesMenuKeyboard();
    private final WeightInputService weightInputService = new WeightInputService();
    // Хранилище для сохранения текущей категории для каждого чата
    private final Map<String, String> chatCategories = new HashMap<>();
    private final ReadExercises readExercises = new ReadExercises();
    private final ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();

    @Autowired
    public TelegramBot(BotProperties botProperties) {
        this.botToken = botProperties.token();
        this.botUsername = botProperties.name();

        // Проверка на пустые значения
        if (botToken.isBlank() || botUsername.isBlank()) {
            throw new IllegalArgumentException("Bot token and username must be specified");
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            SendMessage sendMessage = new SendMessage();
            BotCommand command = BotCommand.fromCommand(text);

            if (command == null) {
                // Если команда не распознана, обрабатываем как выбор упражнения
                handleExerciseSelection(update, sendMessage, text, chatId);
                return;
            }

            switch (command) {
                case START -> {
                    getStartedMenu(update,sendMessage,chatId);
                }
                case FINISH_EXERCISE -> {
                    getFinishExercises(update,sendMessage,chatId);
                }
                case LEGS, CHEST, BACK_MUSCLES -> {
                    handleExerciseCategory(update, sendMessage, text);
                }
                case RESULTS -> {
                    getResultsExercises(update,sendMessage, chatId);
                }
                default -> {
                    handleExerciseSelection(update, sendMessage, text, chatId);
                }
            }
        }
    }
    @SneakyThrows
    private void getStartedMenu (Update update, SendMessage sendMessage, String chatId) {
        sendMessage.setChatId(chatId);
        sendMessage.setText("Меню");
        sendMessage.setReplyMarkup(mainMenu.createReplyKeyboard(update));
        execute(sendMessage);
    }

    @SneakyThrows
    private void getFinishExercises (Update update, SendMessage sendMessage, String chatId) {
        sendMessage.setChatId(chatId);
        sendMessage.setText("Вы закончили тренировку");
        // создаем объект удаления клавиатуры
        removeKeyboard.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(removeKeyboard);
        sendMessage.setReplyMarkup(mainMenu.createReplyKeyboard(update));
        // Показываем главное меню после завершения упражнения
        execute(sendMessage);
        chatCategories.remove(chatId); // Очищаем категорию
    }

    @SneakyThrows
    private void getResultsExercises (Update update, SendMessage sendMessage, String chatId) {
        sendMessage.setChatId(chatId); // Устанавливаем идентификатор чата, куда будет отправлено сообщение
        // Получаем результаты тренировок для конкретного чата из сервиса WeightInputService
        Map<String, Integer> results = weightInputService.getResultsForChat(chatId);
        // Проверяем, есть ли результаты в полученной карте
        if (results.isEmpty()) {
            // Если результатов нет, устанавливаем в сообщение текст об отсутствии результатов
            sendMessage.setText("Результаты пока отсутствуют");
        } else {
            // Если результаты есть, формируем сообщение с ними
            StringBuilder resultMessage = new StringBuilder("Ваши результаты:\n");

            // Проходим по всем парам "упражнение-вес" в карте результатов
            for (Map.Entry<String, Integer> entry : results.entrySet()) {
                // Формируем строку для каждого упражнения
                // entry.getKey() - название упражнения
                // entry.getValue() - вес в килограммах
                resultMessage.append(entry.getKey()) // добавляем название упражнения
                        .append(" - ") // добавляем разделитель
                        .append(entry.getValue()) // добавляем вес
                        .append(" кг\n"); // добавляем единицу измерения и перенос строки
            }
            // Устанавливаем сформированное сообщение в объект SendMessage
            sendMessage.setText(resultMessage.toString());
        }
        execute(sendMessage);
    }

    @SneakyThrows
    private void handleExerciseCategory(Update update, SendMessage sendMessage, String text) {
        String chatId = update.getMessage().getChatId().toString();

        // Сохраняем выбранную категорию
        chatCategories.put(chatId, text);

        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите упражнение");
        sendMessage.setReplyMarkup(massExercisesMenuKeyboard.menuExercisesKeyboard(update));
        execute(sendMessage);
    }

    @SneakyThrows
    private void handleExerciseSelection(Update update, SendMessage sendMessage, String text, String chatId) {
        // Получаем текущую категорию для чата
        String category = chatCategories.get(chatId);

        if (category == null) {
            sendMessage.setChatId(chatId);
            sendMessage.setText("Сначала выберите категорию упражнений");
            execute(sendMessage);
            return;
        }

        List<String> exercises = readExercises.readExercisesFromFile(
                massExercisesMenuKeyboard.getPathExercises(category)
        );

        if (exercises.contains(text)) {
            weightInputService.startWaitingForNumber(chatId, text);
            sendMessage.setChatId(chatId);
            sendMessage.setText("Введите вес для упражнения: " + text);
            execute(sendMessage);
            return;
        }

        if (weightInputService.isWaitingForNumber(chatId) && !update.getMessage().getText().equals(FINISH_EXERCISE.getCommand())) {
            weightInputService.processNumberInput(
                    chatId,
                    text,
                    message -> {
                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
                        }
                    }
            );
        }
    }
}
