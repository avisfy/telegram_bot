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
        int numberOfDay = now.get(Calendar.DAY_OF_WEEK);

        //в пт и сб отсылать ничего не нужно, тк на след. день ничего нет
        if ((numberOfDay != Calendar.FRIDAY) && (numberOfDay != Calendar.SATURDAY)) {
            if ((now.get(Calendar.HOUR_OF_DAY) == 20) && (now.get(Calendar.MINUTE) == 10)) {
                pTBot.completeTaskEvening(chatId, now);
                return;
            }
        }
        if ((numberOfDay != Calendar.SATURDAY) && (numberOfDay != Calendar.SUNDAY)) {
            //test
            Integer number = pTBot.checkTime(numberOfDay, now);
            pTBot.log.info(number.toString());
            if(number != 0)
            {
                int week = (now.get(Calendar.WEEK_OF_YEAR) % 2) == 0 ? 1 : 2;
                pTBot.completeTaskBefore(chatId, number, week, numberOfDay);
            }
        }
    }



}
