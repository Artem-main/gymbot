package MyBot.gymbot.controller;

import MyBot.gymbot.config.properties.BotProperties;
import MyBot.gymbot.keyBoard.ExercisesMenuKeyboard;
import MyBot.gymbot.keyBoard.MainMenu;
import MyBot.gymbot.service.BotCommand;
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


@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;
    private final MainMenu mainMenu = new MainMenu();
    private final ExercisesMenuKeyboard exercisesMenuKeyboard = new ExercisesMenuKeyboard();
    private final WeightInputService weightInputService = new WeightInputService();

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

           switch (command) {
               case START -> {
                   sendMessage.setChatId(chatId);
                   sendMessage.setText("Меню");
                   sendMessage.setReplyMarkup(
                           mainMenu.createReplyKeyboard(update)
                   );
                   execute(sendMessage);
               }
               case FINISH_EXERCISE -> {
                   sendMessage.setChatId(chatId);
                   sendMessage.setText("Вы закончили упражнение");
                   sendMessage.setReplyMarkup(new ReplyKeyboardRemove());
                   sendMessage.setReplyMarkup(
                           mainMenu.createReplyKeyboard(update)
                   );
                   execute(sendMessage);
               }
               case LEGS -> {
                   sendMessage.setChatId(chatId);
                   sendMessage.setText("Упражнения для ног");
                   sendMessage.setReplyMarkup(
                           exercisesMenuKeyboard.menuExercisesKeyboard(update)
                   );
                   execute(sendMessage);
               }
               case CHEST -> {
                   sendMessage.setChatId(chatId);
                   sendMessage.setText("Упражнения для груди");
                   sendMessage.setReplyMarkup(
                           exercisesMenuKeyboard.menuExercisesKeyboard(update)
                   );
                   execute(sendMessage);
               }
               case BACK_MUSCLES -> {
                   sendMessage.setChatId(chatId);
                   sendMessage.setText("Упражнения для спины");
                   sendMessage.setReplyMarkup(
                           exercisesMenuKeyboard.menuExercisesKeyboard(update)
                   );
                   execute(sendMessage);
               }
               case EXERCISE_1 -> {
                   weightInputService.startWaitingForNumber(chatId);
                   sendMessage.setChatId(chatId);
                   sendMessage.setText("Введите вес");
                   execute(sendMessage);
               }
               default -> {
                   if (weightInputService.isWaitingForNumber(chatId) && !update.getMessage().getText().equals("Закончить упражнение")) {
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
       }
    }
}
