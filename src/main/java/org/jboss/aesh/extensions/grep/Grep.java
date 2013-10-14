package org.jboss.aesh.extensions.grep;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandInvocation;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.util.FileLister;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "grep",
        description = "[OPTION]... PATTERN [FILE]...\n"+
                "Search for PATTERN in each FILE or standard input.\n"+
                "PATTERN is, by default, a basic regular expression (BRE).\n" +
                "Example: grep -i 'hello world' menu.h main.c\n")
public class Grep implements Command {

    @Option(shortName = 'H', name = "help", hasValue = false,
            description = "display this help and exit")
    private boolean help;

    @Option(shortName = 'E', name = "extended-regexp", hasValue = false,
            description = "PATTERN is an extended regular expression (ERE)")
    private boolean extendedRegex;

    @Option(shortName = 'F', name = "fixed-strings", hasValue = false,
            description = "PATTERN is a set of newline-separated fixed strings")
    private boolean fixedStrings;

    @Option(shortName = 'G', name = "basic-regexp", hasValue = false,
            description = "PATTERN is a basic regular expression (BRE)")
    private boolean basicRegexp;

    @Option(shortName = 'P', name = "perl-regexp", hasValue = false,
            description = "PATTERN is a Perl regular expression")
    private boolean perlRegexp;

    @Option(shortName = 'e', name = "regexp", argument = "PATTERN",
            description = "use PATTERN for matching")
    private String regexp;

    @Option(shortName = 'f', name = "file", argument = "FILE",
            description = "obtain PATTERN from FILE")
    private File file;

    @Option(shortName = 'i', name = "ignore-case", hasValue = false,
    description = "ignore case distinctions")
    private boolean ignoreCase;

    @Arguments(completer = GrepCompletor.class)
    private List<String> arguments;

    private String pattern;

    public List<String> getArguments() {
        return arguments;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws IOException {
        //just display help and return
        if(help) {
            commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("grep"));
            return CommandResult.SUCCESS;
        }
        //do we have data from a pipe/redirect?
        if(commandInvocation.in().getStdIn().available() > 0) {
            java.util.Scanner s = new java.util.Scanner(commandInvocation.in().getStdIn()).useDelimiter("\\A");
            String input = s.hasNext() ? s.next() : "";
            List<String> inputLines = new ArrayList<String>();
            for(String i : input.split("\n"))
                inputLines.add(i);

            if(arguments != null && arguments.size() > 0)
                pattern = arguments.remove(0);

            doGrep(inputLines, commandInvocation.getShell());
        }
        //find argument files and build regex..
        else {
            if(arguments != null && arguments.size() > 0)
                pattern = arguments.remove(0);
            if(arguments != null && arguments.size() > 0) {
                for(String s : arguments)
                    doGrep(new File(s), commandInvocation.getShell());
            }
            //posix starts an interactive shell and read from the input here
            //atm, we'll just quit
            else {
                return CommandResult.SUCCESS;
            }

        }

        return null;
    }

    private void doGrep(File file, Shell shell) {
        if(!file.exists()) {
            shell.out().println("grep: "+file.toString()+": No such file or directory");
        }
        else if(file.isFile()) {
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader br = new BufferedReader(fileReader);
                List<String> inputLines = new ArrayList<String>();

                String line;
                while ((line = br.readLine()) != null) {
                    inputLines.add(line);
                }

                doGrep(inputLines, shell);
            }
            catch (FileNotFoundException fnf) {
                fnf.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void doGrep(List<String> inputLines, Shell shell) {
        if(pattern != null) {
            for(String line : inputLines) {
                if(line != null && line.contains(pattern)) {
                    shell.out().println(line);
                }
            }
        }
        else
            shell.out().println("No pattern given");

    }

    /**
     * First argument is the pattern
     * All other arguments should be files
     */
    public static class GrepCompletor implements OptionCompleter {

        @Override
        public void complete(CompleterData completerData) {
            Grep grep = (Grep) completerData.getCommand();
            //the first argument is the pattern, do not autocomplete
            if(grep.getArguments() == null || grep.getArguments().size() == 0) {
                return;
            }
            else if(grep.getArguments().size() > 0) {
                File cwd = new File(Config.getUserDir());
                CompleteOperation completeOperation = new CompleteOperation(completerData.getGivenCompleteValue(), 0);
                if (completerData.getGivenCompleteValue() == null)
                    new FileLister("", cwd, FileLister.Filter.ALL).findMatchingDirectories(completeOperation);
                else
                    new FileLister(completerData.getGivenCompleteValue(), cwd, FileLister.Filter.ALL)
                            .findMatchingDirectories(completeOperation);

                if (completeOperation.getCompletionCandidates().size() > 1) {
                    completeOperation.removeEscapedSpacesFromCompletionCandidates();
                }

                completerData.setCompleterValues(completeOperation.getCompletionCandidates());
                if (completerData.getGivenCompleteValue() != null && completerData.getCompleterValues().size() == 1) {
                    completerData.setAppendSpace(completeOperation.hasAppendSeparator());
                }
            }
        }
    }
}
