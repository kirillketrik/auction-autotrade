package ru.geroldina.ftauctionbot.client.domain.auction;

import org.junit.jupiter.api.Test;
import ru.geroldina.ftauctionbot.client.domain.auction.model.PageInfo;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultAuctionScreenAnalyzerTest {
    private final DefaultAuctionScreenAnalyzer analyzer = new DefaultAuctionScreenAnalyzer();

    @Test
    void parsesPageInfoFromFuntimeTitle() {
        Optional<PageInfo> pageInfo = analyzer.parsePageInfo("0A2z󏾑15/1587󏾦                  Аукцион                  ");

        assertEquals(Optional.of(new PageInfo(15, 1587)), pageInfo);
    }

    @Test
    void recognizesSearchAuctionTitleWithoutAuctionWord() {
        Optional<PageInfo> pageInfo = analyzer.parsePageInfo("0A2z󏾘1/4󏾮             漢:Святая вода             ");

        assertEquals(Optional.of(new PageInfo(1, 4)), pageInfo);
        assertEquals(true, analyzer.isAuctionScreenTitle("0A2z󏾘1/4󏾮             漢:Святая вода             "));
    }
}
