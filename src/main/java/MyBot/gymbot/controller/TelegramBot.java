package MyBot.gymbot.controller;

import MyBot.gymbot.Data.DatabaseHelper;
import MyBot.gymbot.ExercisesKeyboard.CardioTraining;
import MyBot.gymbot.config.properties.BotProperties;
import MyBot.gymbot.ExercisesKeyboard.MassExercisesMenuKeyboard;
import MyBot.gymbot.ExercisesKeyboard.MainMenu;
import MyBot.gymbot.service.*;
import MyBot.gymbot.utils.ReadExercisesUtils;
import MyBot.gymbot.utils.ReadFilesUtils;
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

import static MyBot.gymbot.service.BotCommand.*;


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
    private final ReadExercisesUtils readExercisesUtils = new ReadExercisesUtils();
    private final ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
    private final CardioTraining cardioTraining = new CardioTraining();
    private final ReadFilesUtils readFilesUtils = new ReadFilesUtils();
    private final DatabaseHelper dbHelper = new DatabaseHelper();


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
            Long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getFrom().getUserName();

            SendMessage sendMessage = new SendMessage();
            BotCommand command = fromCommand(text);

            // Сначала проверяем есть ID в базе
            if (dbHelper.getUser(chatId) == null) {
                // Сохраняем или обновляем информацию о пользователе
                dbHelper.addUser(chatId, username);
                sendMessage(chatId, "Привет, " + (username != null ? "@" + username : "пользователь") + "!\n" +
                        "Ваш ID сохранен в базе данных.");
            }

            if (command == null) {
                // Если команда не распознана, обрабатываем как выбор упражнения
                handleExerciseSelection(update, sendMessage, text, chatId);
                return;
            }
//        STRENGHT
//        WEIGHT_LOSS
//        NUTRITION
//        FUNCTIONAL
//        FAT_BURNING
//        WARM_UP

            switch (command) {
                case START, BACK_ON_MENU -> {
                    getStartedMenu(update,sendMessage,chatId);
                }
                case WEIGHT_GAIN -> {
                    sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(readFilesUtils.readTextFromFile(
                            WEIGHT_GAIN.getFilePath()
                    ));

                    sendMessage.setReplyMarkup(massExercisesMenuKeyboard
                            .showCategoriesKeyboard(update));
                    execute(sendMessage);
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
                case CARDIO -> {
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Выберите меню");
                    sendMessage.setReplyMarkup(cardioTraining.cardioKeyboard(update));
                    execute(sendMessage);
                }
                case CARDIO_TRAINING -> {
                    sendMessage.setChatId(chatId);
                    String exe = readFilesUtils.readTextFromFile(
                            cardioTraining.getPathCardioCategory(CARDIO_TRAINING.getCommand()));
                    sendMessage.setText(exe);
                    execute(sendMessage);

                }
                case CARDIO_RECOMMENDATIONS -> {
                    sendMessage.setChatId(chatId);
                    String exercises = readFilesUtils.readTextFromFile(
                            cardioTraining.getPathCardioCategory(CARDIO_RECOMMENDATIONS.getCommand()));
                    sendMessage.setText(exercises);
                    execute(sendMessage);
                }
                default -> {
                    handleExerciseSelection(update, sendMessage, text, chatId);
                }
            }
        }
    }

    @SneakyThrows
    private void getStartedMenu (Update update, SendMessage sendMessage, Long chatId) {
        final String readMainMenuText = readFilesUtils.readTextFromFile(START.getFilePath());
        sendMessage.setChatId(chatId);
        sendMessage.setText(readMainMenuText);
        sendMessage.setReplyMarkup(mainMenu.createReplyKeyboard(update));
        execute(sendMessage);
    }

    @SneakyThrows
    private void getFinishExercises (Update update, SendMessage sendMessage, Long chatId) {
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
    private void getResultsExercises (Update update, SendMessage sendMessage, Long chatId) {
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
    private void handleExerciseSelection(Update update, SendMessage sendMessage, String text, Long chatId) {
        // Получаем текущую категорию для чата
        String category = chatCategories.get(chatId.toString());

        if (category == null) {
            sendMessage.setChatId(chatId);
            sendMessage.setText("Сначала выберите категорию упражнений");
            execute(sendMessage);
            return;
        }

        List<String> exercises = readExercisesUtils.readExercisesFromFile(
                massExercisesMenuKeyboard.getPathExercises(category)
        );

        if (exercises.contains(text)) {
            // Запускаем режим ожидания ввода веса
            weightInputService.startWaitingForNumber(chatId, text);
            sendMessage.setChatId(chatId);
            sendMessage.setText("Введите вес для упражнения: " + text);
            execute(sendMessage);
            return;
        }

        // Проверяем, ожидает ли сервис ввода числа
        if (weightInputService.isWaitingForNumber(chatId) && !update.getMessage().getText().equals(FINISH_EXERCISE.getCommand())) {
            // Обрабатываем введенный вес
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
        } else {
            // Если введенный текст не является числом и не является командой завершения
            sendMessage.setChatId(chatId);
            sendMessage.setText("Пожалуйста, введите числовое значение веса");
            execute(sendMessage);
        }
    }

    @SneakyThrows
    private void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        execute(sendMessage);
    }

    @Override
    public void onClosing() {
        dbHelper.close();
    }

}
