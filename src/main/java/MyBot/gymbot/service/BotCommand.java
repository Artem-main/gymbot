package MyBot.gymbot.service;

import java.util.Arrays;

public enum BotCommand {
    START("/start"),
    MAIN_MENU("/main"),
    CLEAR("/clear"),
    FINISH_EXERCISE("Закончить упражнение"),
    BACK_MUSCLES("Спина"),
    CHEST("Грудь"),
    LEGS("Ноги"),
    EXERCISE_1("Жим гантелей стоя - 4х10");

    private final String command;

    BotCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    // Метод для поиска команды по тексту
    public static BotCommand fromCommand(String command) {
        return Arrays.stream(values())
                .filter(c -> c.getCommand().equals(command))
                .findFirst()
                .orElse(null);
    }
}
