package MyBot.gymbot.keyBoard;

import MyBot.gymbot.service.ReadExercises;
import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static MyBot.gymbot.service.BotCommand.*;

public class ExercisesMenuKeyboard {

    private final ReadExercises readExercises = new ReadExercises();
    private final SendMessage sendMessageBack = new SendMessage();

    @SneakyThrows
    public ReplyKeyboardMarkup menuExercisesKeyboard(Update update) {

        sendMessageBack.setChatId(update.getMessage().getChatId().toString());
        sendMessageBack.setText("Выберите упражнение для спины:");

        // Читаем список упражнений из файла
        List<String> exercises = readExercises.readExercisesFromFile(getPathExercises(update.getMessage().getText()));

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
        keyboard.setKeyboard(keyboardRows);

        KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add(FINISH_EXERCISE.getCommand());
        keyboardRows.add(keyboardButtons);
        keyboard.setKeyboard(keyboardRows);

        return keyboard;
    }

    public String getPathExercises(String text) {
        if (text.equals(BACK_MUSCLES.getCommand())) {
            return "src/main/resources/trainingFiles/back.txt";
        } else if (text.equals(LEGS.getCommand())) {
            return "src/main/resources/trainingFiles/legs.txt";
        } else if (text.equals(CHEST.getCommand())) {
            return "src/main/resources/trainingFiles/chest.txt";
        }
        return null;
    }
}
