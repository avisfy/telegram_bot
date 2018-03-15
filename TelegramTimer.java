import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

public class TelegramTimer extends TimerTask {
    private String chatId;
    private TelegramBot pTBot;

    public TelegramTimer(String chatId, TelegramBot pTBot)
    {
        this.chatId = chatId;
        this.pTBot = pTBot;
    }

    @Override
    public void run() {
        Calendar now;
        now = Calendar.getInstance();
        Integer numberOfDay = now.get(Calendar.DAY_OF_WEEK);
        //в пт и сб отсылать ничего не нужно, тк на след. день ничего нет
        if ((numberOfDay == Calendar.FRIDAY) || (numberOfDay == Calendar.SATURDAY)) {
            return;
        } else if ((now.get(Calendar.HOUR_OF_DAY) == 21) && (now.get(Calendar.MINUTE) == 10)){
            pTBot.completeTask(chatId, now);
        }
    }

}
