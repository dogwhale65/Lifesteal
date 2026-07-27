package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.Constants;
import net.minecraft.world.Container;

import java.util.ArrayList;
import java.util.List;

public class PageManager {

    private int currentPage = 0;

    public void populateCurrentPage(Container inventory, List<PlayerCollector.Revivable> revivables,
                                    ReviveItemFactory factory, ReviveSort sort) {
        for (int i = 0; i < Constants.CHEST_6X9_SIZE; i++) {
            inventory.setItem(i, factory.createFiller());
        }

        List<PlayerCollector.Revivable> entries = new ArrayList<>(revivables);
        sort.apply(entries);

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / Constants.ITEMS_PER_PAGE));

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        int startIndex = currentPage * Constants.ITEMS_PER_PAGE;
        int endIndex   = Math.min(startIndex + Constants.ITEMS_PER_PAGE, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            PlayerCollector.Revivable entry = entries.get(i);
            inventory.setItem(slotForPageIndex(i - startIndex),
                    factory.createPlayerHead(entry.name(), entry.banned()));
        }

        if (currentPage > 0) {
            inventory.setItem(Constants.SLOT_PREV_PAGE, factory.createPreviousPageButton());
        }
        inventory.setItem(Constants.SLOT_PAGE_INFO, factory.createPageInfo(currentPage + 1, totalPages));
        inventory.setItem(Constants.SLOT_SORT, factory.createSortButton(sort));
        if (currentPage < totalPages - 1) {
            inventory.setItem(Constants.SLOT_NEXT_PAGE, factory.createNextPageButton());
        }
    }

    /** Maps a head's position on the page onto its chest slot inside the padded head area. */
    private static int slotForPageIndex(int index) {
        int row = Constants.HEAD_FIRST_ROW + index / Constants.HEAD_COLUMNS;
        int col = Constants.HEAD_FIRST_COL + index % Constants.HEAD_COLUMNS;
        return row * Constants.GRID_COLUMNS + col;
    }

    public boolean navigateToPreviousPage() {
        if (currentPage <= 0) return false;
        currentPage--;
        return true;
    }

    public boolean navigateToNextPage(int totalEntries) {
        int totalPages = (int) Math.ceil((double) totalEntries / Constants.ITEMS_PER_PAGE);
        if (currentPage >= totalPages - 1) return false;
        currentPage++;
        return true;
    }
}

