package Project.Client.Views;

import javax.swing.*;
import java.awt.*;

import Project.Client.Client;
import Project.Client.Interfaces.IGameSettingEvents;
import Project.Client.Interfaces.IHostEvents;

// agl8 12-19-2025
// ready check UI with host-only toggles for RPS-5 and cooldown
public class ReadyView extends JPanel implements IHostEvents, IGameSettingEvents {
    private final JButton readyButton = new JButton("Ready");
    private final JCheckBox rps5Toggle = new JCheckBox("Enable RPS-5 (Lizard/Spock)");
    private final JCheckBox cooldownToggle = new JCheckBox("Enable Cooldown (no repeat pick)");

    private final JLabel hostLabel = new JLabel("Host: (unknown)");

    public ReadyView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(hostLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        add(rps5Toggle);
        add(cooldownToggle);
        add(Box.createRigidArea(new Dimension(0, 10)));

        add(readyButton);

        // default: disabled until host info arrives
        rps5Toggle.setEnabled(false);
        cooldownToggle.setEnabled(false);

        readyButton.addActionListener(e -> {
            try {
                Client.INSTANCE.sendReady();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Only host can *send* updates; non-host sees but cannot interact
        rps5Toggle.addActionListener(e -> {
            if (Client.INSTANCE.isHost()) {
                try {
                    Client.INSTANCE.sendGameSettings(rps5Toggle.isSelected(), cooldownToggle.isSelected());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        cooldownToggle.addActionListener(e -> {
            if (Client.INSTANCE.isHost()) {
                try {
                    Client.INSTANCE.sendGameSettings(rps5Toggle.isSelected(), cooldownToggle.isSelected());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Register for callbacks
        Client.INSTANCE.registerCallback(this);
    }

    @Override
    public void onHostUpdate(long hostClientId) {
        hostLabel.setText("Host ClientId: " + hostClientId);

        boolean enable = Client.INSTANCE.isHost();
        rps5Toggle.setEnabled(enable);
        cooldownToggle.setEnabled(enable);
    }

    @Override
    public void onGameSettingsUpdate(boolean enableRps5, boolean enableCooldown) {
        rps5Toggle.setSelected(enableRps5);
        cooldownToggle.setSelected(enableCooldown);

        boolean enable = Client.INSTANCE.isHost();
        rps5Toggle.setEnabled(enable);
        cooldownToggle.setEnabled(enable);
    }
}
