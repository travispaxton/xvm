/**
 * The Date value type is used to represent the information about a date in Gregorian form.
 */
const Date(Int year, Int month, Int day)
    {
    @ro dayOfYear;
    enum DayOfWeek(Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday);


    Time add(Duration);
    Duration sub(Time);
    Time sub(Duration);

    DateTime to<DateTime>()
        {
        return new DateTime(this, Time:"00:00");
        }
    }
