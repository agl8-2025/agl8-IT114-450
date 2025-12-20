package Project.Client.Views;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import Project.Client.Client;
import Project.Common.User;

// agl8 12-19-2025
// user row with name, id, points, and status indicators
public class UserListItem extends JPanel {
    private final long clientId;

    private final JLabel nameLabel = new JLabel();
    private final JLabel pointsLabel = new JLabel("0 pts");
    private final JLabel statusLabel = new JLabel("");

    private final JButton awayButton = new JButton("Away");

    // cached statuses
    private boolean isAway = false;
    private boolean isEliminated = false;
    private boolean isReady = false;
    private boolean didPickThisRound = false;

    // shared phase state (simple static, like professor example)
    private static boolean inProgress = false;

    public UserListItem(long clientId) {
        this.clientId = clientId;

        setLayout(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        left.add(nameLabel);
        left.add(statusLabel);

        right.add(pointsLabel);

        // only show away toggle for yourself
        if (Client.INSTANCE.isMyClientIdSet() && Client.INSTANCE.getMyClientId() == clientId) {
            right.add(awayButton);
            awayButton.addActionListener(e -> {
                try {
                    Client.INSTANCE.sendAwayToggle();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        refresh();
    }

    public long getClientId() {
        return clientId;
    }

    public void setPhase(String phase) {
        // match your repo's phase names; treat anything not READY as in-progress
        inProgress = phase != null && !phase.equalsIgnoreCase("READY");
        refresh();
    }

    public void setPoints(int pts) {
        pointsLabel.setText(pts + " pts");
        refresh();
    }

    public void setAway(boolean away) {
        isAway = away;
        awayButton.setText(isAway ? "Back" : "Away");
        refresh();
    }

    public void setEliminated(boolean eliminated) {
        isEliminated = eliminated;
        refresh();
    }

    public void setReady(boolean ready) {
        isReady = ready;
        refresh();
    }

    public void setDidPick(boolean didPick) {
        didPickThisRound = didPick;
        refresh();
    }

    private void refresh() {
        User u = Client.INSTANCE.getUser(clientId);
        String name = (u != null) ? u.getDisplayName() : Client.INSTANCE.getDisplayNameFromId(clientId);

        // Requirement: show username AND id
        nameLabel.setText(name + " (#" + clientId + ")");

        // Build status tags
        StringBuilder sb = new StringBuilder("  ");

        // Spectator definition for this project:
        // In-progress + not ready => spectator
        boolean spectator = inProgress && !isReady;

        if (isEliminated) sb.append("[ELIM] ");
        if (isAway) sb.append("[AWAY] ");
        if (spectator) sb.append("[SPEC] ");

        // Pending-to-pick:
        // In-progress, ready, not eliminated, not away, and has not picked yet
        boolean pending = inProgress && isReady && !isEliminated && !isAway && !didPickThisRound;
        if (pending) sb.append("[PENDING] ");

        statusLabel.setText(sb.toString());

        // Visual styles:
        setOpaque(true);

        if (isEliminated) {
            setBackground(Color.LIGHT_GRAY);
        } else if (isAway) {
            setBackground(new Color(210, 210, 210));
        } else {
            setBackground(Color.WHITE);
        }

        if (spectator) {
            nameLabel.setForeground(new Color(170, 120, 0)); // yellow-ish
        } else {
            nameLabel.setForeground(Color.BLACK);
        }
    }
}