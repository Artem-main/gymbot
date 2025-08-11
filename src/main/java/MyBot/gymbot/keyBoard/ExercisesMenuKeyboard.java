package MyBot.gymbot.keyBoard;

import MyBot.gymbot.service.BotCommand;
import MyBot.gymbot.service.ReadExercises;
import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class ExercisesMenuKeyboard {
    private final ReadExercises readExercises = new ReadExercises();
    private final SendMessage sendMessageBack = new SendMessage();

    @SneakyThrows
    public ReplyKeyboardMarkup menuExercisesKeyboard(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String category = getCategoryFromUpdate(update);

        sendMessageBack.setChatId(chatId);
        sendMessageBack.setText("Выберите упражнение для " + category);

        // Читаем список упражнений из файла
        List<String> exercises = readExercises.readExercisesFromFile(getPathExercises(category));

        // Создаем объект клавиатуры
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        // Создаем список строк для клавиатуры
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Для каждой кнопки создаем отдельную строку
        for (String exercise : exercises) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(exercise));
            keyboardRows.add(row);
        }

        // Добавляем кнопку завершения
        KeyboardRow finishRow = new KeyboardRow();
        finishRow.add(BotCommand.FINISH_EXERCISE.getCommand());
        keyboardRows.add(finishRow);

        keyboard.setKeyboard(keyboardRows);
        return keyboard;
    }

    private String getCategoryFromUpdate(Update update) {
        // Получаем предыдущее сообщение, чтобы определить категорию
        String text = update.getMessage().getText();
        if (text.equals(BotCommand.BACK_MUSCLES.getCommand())) {
            return "Спина";
        } else if (text.equals(BotCommand.LEGS.getCommand())) {
            return "Ноги";
        } else if (text.equals(BotCommand.CHEST.getCommand())) {
            return "Грудь";
        }
        return null; // значение по умолчанию
    }

    public String getPathExercises(String category) {
        return switch (category) {
            case "Спина" -> "src/main/resources/trainingFiles/back.txt";
            case "Ноги" -> "src/main/resources/trainingFiles/legs.txt";
            case "Грудь" -> "src/main/resources/trainingFiles/chest.txt";
            default -> null;
        };
    }
}

