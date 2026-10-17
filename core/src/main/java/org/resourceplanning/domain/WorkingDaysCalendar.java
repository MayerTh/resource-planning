package org.resourceplanning.domain;

import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;

public final class WorkingDaysCalendar {

    private final Map<YearMonth, Integer> countsByMonth;

    public WorkingDaysCalendar(Map<YearMonth, Integer> countsByMonth) {
        this.countsByMonth = Map.copyOf(countsByMonth);
    }

    public int workingDays(YearMonth month) {
        Integer count = countsByMonth.get(month);
        if (count == null) {
            throw new NoSuchElementException("No working-days entry for " + month);
        }
        return count;
    }

    public boolean hasEntry(YearMonth month) {
        return countsByMonth.containsKey(month);
    }
}
