import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

public class telegramTimer extends TimerTask {

    private static String mes;
    private static String chatId;

    public static void setChatId(String chatId) {
        telegramTimer.chatId = chatId;
    }

    @Override
    public void run() {
        Calendar now;
        now = Calendar.getInstance();
        Integer numberOfDay = now.get(Calendar.DAY_OF_WEEK);
        //в пт и сб отсылать ничего не нужно, тк на след. день ничего нет
        if ((numberOfDay == Calendar.FRIDAY) || (numberOfDay == Calendar.SATURDAY)) {
            return;
        } else if ((now.get(Calendar.HOUR_OF_DAY) == 15) && (now.get(Calendar.MINUTE) == 55)){
            TelegramBot.completeTask(chatId, now);
        }
    }

}
