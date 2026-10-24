package es.upm.miw.oneleft.social;

import es.upm.miw.oneleft.social.domain.model.BlockedPerson;
import es.upm.miw.oneleft.social.domain.model.PageQuery;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.Report;
import es.upm.miw.oneleft.social.domain.model.ReportStatus;
import es.upm.miw.oneleft.social.domain.model.ReportTarget;
import es.upm.miw.oneleft.social.domain.port.out.Blocks;
import es.upm.miw.oneleft.social.domain.port.out.Reports;
import es.upm.miw.oneleft.social.domain.port.out.SafetyEvents;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Blocks, reports and the events about them in memory, with the same rules as the SQL queries (tested in
 * SafetyIntegrationTest). The names of the blocked people come from the people of {@link InMemorySocial}.
 */
public class InMemorySafety implements Blocks, Reports, SafetyEvents {

    public final Map<String, Instant> blocks = new LinkedHashMap<>();
    public final Map<UUID, Report> reports = new LinkedHashMap<>();
    public final List<String> events = new ArrayList<>();
    private final InMemorySocial social;

    public InMemorySafety(InMemorySocial social) {
        this.social = social;
    }

    private static String key(UUID blockerId, UUID blockedId) {
        return blockerId + ">" + blockedId;
    }

    @Override
    public boolean block(UUID blockerId, UUID blockedId, Instant at) {
        return blocks.putIfAbsent(key(blockerId, blockedId), at) == null;
    }

    @Override
    public boolean unblock(UUID blockerId, UUID blockedId) {
        return blocks.remove(key(blockerId, blockedId)) != null;
    }

    @Override
    public boolean blocked(UUID blockerId, UUID blockedId) {
        return blocks.containsKey(key(blockerId, blockedId));
    }

    @Override
    public PageResult<BlockedPerson> blockedBy(UUID blockerId, PageQuery page) {
        var all = blocks.entrySet().stream().filter(entry -> entry.getKey().startsWith(blockerId + ">"))
                .map(entry -> {
                    var blockedId = UUID.fromString(entry.getKey().substring(entry.getKey().indexOf('>') + 1));
                    return new BlockedPerson(blockedId, social.people.get(blockedId).name(), entry.getValue());
                })
                .sorted(Comparator.comparing(BlockedPerson::since).reversed().thenComparing(BlockedPerson::name))
                .toList();
        var from = Math.min(all.size(), page.page() * page.size());
        return new PageResult<>(all.subList(from, Math.min(all.size(), from + page.size())), page.page(), page.size(),
                all.size());
    }

    @Override
    public Report save(Report report) {
        reports.put(report.id(), report);
        return report;
    }

    @Override
    public Optional<Report> findOpen(UUID reporterId, ReportTarget target, UUID targetId) {
        return reports.values().stream().filter(report -> report.reporterId().equals(reporterId)
                && report.target() == target && report.targetId().equals(targetId)
                && report.status() == ReportStatus.OPEN).findFirst();
    }

    @Override
    public PageResult<Report> open(PageQuery page) {
        var all = reports.values().stream().filter(report -> report.status() == ReportStatus.OPEN)
                .sorted(Comparator.comparing(Report::reportedAt)).toList();
        var from = Math.min(all.size(), page.page() * page.size());
        return new PageResult<>(all.subList(from, Math.min(all.size(), from + page.size())), page.page(), page.size(),
                all.size());
    }

    @Override
    public void blocked(UUID blockerId, UUID blockedId, Instant at) {
        events.add("user.blocked " + blockerId + ">" + blockedId);
    }

    @Override
    public void unblocked(UUID blockerId, UUID blockedId, Instant at) {
        events.add("user.unblocked " + blockerId + ">" + blockedId);
    }

    @Override
    public void reported(Report report) {
        events.add("report.created " + report.id());
    }
}
