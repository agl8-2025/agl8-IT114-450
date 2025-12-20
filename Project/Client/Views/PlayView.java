package Project.Client.Views;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import Project.Client.Client;
import Project.Client.Interfaces.IGameSettingEvents;
import Project.Common.Phase;

// agl8 12-19-2025
// game area for RPS picks with RPS-5 and cooldown support
public class PlayView extends JPanel implements IGameSettingEvents {
    private final JPanel buttonPanel = new JPanel(new FlowLayout());

    private final Map<String, JButton> buttons = new LinkedHashMap<>();
    private boolean enableRps5 = false;
    private boolean enableCooldown = false;

    public PlayView() {
        this(null);
    }

    // agl8 12-19-2025
    // constructor overload for GameView compatibility
    public PlayView(String name) {
        setLayout(new BorderLayout());
        if (name != null) setName(name);
        add(buttonPanel, BorderLayout.CENTER);

        // Always-available RPS
        addChoiceButton("r", "Rock");
        addChoiceButton("p", "Paper");
        addChoiceButton("s", "Scissors");

        // RPS-5 extras (created but only shown when enabled)
        addChoiceButton("l", "Lizard");
        addChoiceButton("k", "Spock");

        rebuildButtons();
        
        Client.INSTANCE.registerCallback(this);
    }

    private void addChoiceButton(String code, String label) {
        JButton b = new JButton(label);
        b.addActionListener(e -> {
            try {
                Client.INSTANCE.sendPick(code);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Note: we do NOT update lastChoice locally here; server will confirm via CHOICE payload.
        });
        buttons.put(code, b);
    }

    private void rebuildButtons() {
        buttonPanel.removeAll();

        buttonPanel.add(buttons.get("r"));
        buttonPanel.add(buttons.get("p"));
        buttonPanel.add(buttons.get("s"));

        if (enableRps5) {
            buttonPanel.add(buttons.get("l"));
            buttonPanel.add(buttons.get("k"));
        }

        applyCooldownDisable();
        revalidate();
        repaint();
    }

    private void applyCooldownDisable() {
        for (JButton b : buttons.values()) b.setEnabled(true);

        if (!enableCooldown) return;

        String last = Client.INSTANCE.getMyLastChoice();
        if (last == null) return;

        JButton toDisable = buttons.get(last);
        if (toDisable != null) {
            toDisable.setEnabled(false);
        }
    }

    @Override
    public void onGameSettingsUpdate(boolean enableRps5, boolean enableCooldown) {
        this.enableRps5 = enableRps5;
        this.enableCooldown = enableCooldown;
        rebuildButtons();
    }

    // agl8 12-19-2025
    // phase change handler for GameView compatibility
    public void changePhase(Phase phase) {
        // rebuild buttons when phase changes to apply cooldown state
        rebuildButtons();
    }
}
