package com.AYLUS.DiscordBot.Classes;

import java.util.Collections;
import java.util.List;

public class LeaderboardPagination {
    private final List<UserVolunteerProfile> fullList;
    private final int itemsPerPage;

    public LeaderboardPagination(List<UserVolunteerProfile> fullList, int itemsPerPage) {
        this.fullList = fullList;
        this.itemsPerPage = itemsPerPage;
    }

    public List<UserVolunteerProfile> getPage(int pageIndex) {
        int fromIndex = pageIndex * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, fullList.size());

        if (fromIndex >= fullList.size()) {
            return Collections.emptyList();
        }

        return fullList.subList(fromIndex, toIndex);
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) fullList.size() / itemsPerPage);
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    // Add this getter
    public List<UserVolunteerProfile> getFullList() {
        return Collections.unmodifiableList(fullList);
    }
}