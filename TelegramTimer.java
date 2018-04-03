import java.util.Calendar;
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
        now.setFirstDayOfWeek(Calendar.MONDAY);
        int numberOfDay = now.get(Calendar.DAY_OF_WEEK);

        //test
        //now.set(Calendar.HOUR_OF_DAY, 20);
        //now.set(Calendar.MINUTE, 10);
        //now.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        //в пт и сб отсылать ничего не нужно, тк на след. день ничего нет
        if ((numberOfDay != Calendar.FRIDAY) && (numberOfDay != Calendar.SATURDAY)) {
            if ((now.get(Calendar.HOUR_OF_DAY) == 20) && (now.get(Calendar.MINUTE) == 10)) {
                pTBot.completeTaskNextDay(chatId, now, TelegramBot.TYPE_TABLE.SMALL);
                return;
            }
        }
        if ((numberOfDay != Calendar.SATURDAY) && (numberOfDay != Calendar.SUNDAY)) {
            //test
            //now.set(Calendar.HOUR_OF_DAY, 7);
            //now.set(Calendar.MINUTE, 45);
            //now.set(Calendar.HOUR_OF_DAY, 12);
            //now.set(Calendar.MINUTE, 55);
            Integer number = pTBot.checkTime(now);
            if(number != 0)
            {
                int week = (now.get(Calendar.WEEK_OF_YEAR) % 2) == 0 ? 1 : 2;
                pTBot.completeTaskBefore(chatId, number, week, numberOfDay);
            }
        }
    }



}
