package Project.Client.Views;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import Project.Client.Client;
import Project.Client.Interfaces.*;
import Project.Common.Phase;
import Project.Common.User;
import Project.Common.Constants;

// agl8 12-19-2025
// user list panel sorted by points with status updates
public class UserListView extends JPanel implements IRoomEvents, IReadyEvent, ITurnEvent, IPointsEvent, IAwayEvents, IEliminationEvents, IPhaseEvent {
    private final Map<Long, UserListItem> items = new HashMap<>();

    public UserListView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Client.INSTANCE.registerCallback(this);
    }

    private void rebuildSorted() {
        SwingUtilities.invokeLater(() -> {
            removeAll();

            List<UserListItem> list = new ArrayList<>(items.values());

            list.sort((a, b) -> {
                User ua = Client.INSTANCE.getUser(a.getClientId());
                User ub = Client.INSTANCE.getUser(b.getClientId());

                int pa = (ua == null) ? 0 : ua.getPoints();
                int pb = (ub == null) ? 0 : ub.getPoints();

                if (pa != pb) return Integer.compare(pb, pa); // desc points

                String na = (ua == null) ? "" : ua.getDisplayName();
                String nb = (ub == null) ? "" : ub.getDisplayName();
                return na.compareToIgnoreCase(nb); // asc name
            });

            for (UserListItem item : list) add(item);

            revalidate();
            repaint();
        });
    }

    private UserListItem getOrCreate(long clientId) {
        return items.computeIfAbsent(clientId, id -> new UserListItem(id));
    }

    @Override
    public void onRoomAction(long clientId, String message, boolean isJoin, boolean isQuiet) {
        // If DEFAULT_CLIENT_ID (-1) to reset list, handle it:
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            items.clear();
            rebuildSorted();
            return;
        }

        if (isJoin) {
            getOrCreate(clientId);
        } else {
            items.remove(clientId);
        }
        rebuildSorted();
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // unused for list
    }

    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            // reset all
            for (UserListItem item : items.values()) {
                item.setReady(false);
                item.setDidPick(false);
            }
            rebuildSorted();
            return;
        }
        getOrCreate(clientId).setReady(isReady);
        rebuildSorted();
    }

    @Override
    public void onTookTurn(long clientId, boolean didTakeTurn) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            for (UserListItem item : items.values()) item.setDidPick(false);
            rebuildSorted();
            return;
        }
        getOrCreate(clientId).setDidPick(didTakeTurn);
        rebuildSorted();
    }

    @Override
    public void onPointsUpdate(long clientId, int points) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            rebuildSorted();
            return;
        }
        getOrCreate(clientId).setPoints(points);
        rebuildSorted();
    }

    @Override
    public void onAwayStatusUpdated(long clientId, boolean isAway, boolean isQuiet) {
        getOrCreate(clientId).setAway(isAway);
        rebuildSorted();
    }

    @Override
    public void onEliminationUpdated(long clientId, boolean isEliminated) {
        getOrCreate(clientId).setEliminated(isEliminated);
        rebuildSorted();
    }

    @Override
    public void onReceivePhase(Phase phase) {
        String phaseName = (phase != null) ? phase.name() : "READY";
        for (UserListItem item : items.values()) item.setPhase(phaseName);
        rebuildSorted();
    }
}
