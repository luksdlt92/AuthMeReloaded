package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * Display or change the status of the antibot mod.
 */
public class SwitchAntiBotCommand implements ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments, CommandService commandService) {
        if (arguments.isEmpty()) {
            sender.sendMessage("[AuthMe] AntiBot status: " + AntiBot.getAntiBotStatus().name());
            return;
        }

        String newState = arguments.get(0);

        // Enable or disable the mod
        if ("ON".equalsIgnoreCase(newState)) {
            AntiBot.overrideAntiBotStatus(true);
            sender.sendMessage("[AuthMe] AntiBot Manual Override: enabled!");
        } else if ("OFF".equalsIgnoreCase(newState)) {
            AntiBot.overrideAntiBotStatus(false);
            sender.sendMessage("[AuthMe] AntiBot Manual Override: disabled!");
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "Invalid AntiBot mode!");
            FoundCommandResult result = commandService.mapPartsToCommand(sender, Arrays.asList("authme", "antibot"));
            commandService.outputHelp(sender, result, HelpProvider.SHOW_ARGUMENTS);
            sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/authme help antibot");
        }
    }
}
