/*
 * Copyright 2018-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package in.ac.iitkgp.stan;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.apache.karaf.shell.commands.Argument;
import org.onosproject.fwd.ReactiveForwarding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;

/**
 * Sample Apache Karaf CLI command
 */
@Command(scope = "onos", name = "flowseer",
        description = "FlowSeer CLI command")
public class FlowSeerCommand extends AbstractShellCommand
{
    @Argument(index = 0, name = "cmd", description = "Command",
            required = false, multiValued = false)
    private String cmd = null;

    @Override
    protected void execute()
    {
        ReactiveForwarding fwdService = AbstractShellCommand.get(ReactiveForwarding.class);
        FlowSeer flowSeer = fwdService.flowSeer;
        FlowDataList flowDataList = fwdService.isFlowSeerActive ? flowSeer.getFlowDataList() : null;

        if (cmd == null)
        {
            System.out.println("Please specify an option.");
            System.out.println("Type \"flowseer help\" to view available options.");
        }
        else if (cmd.equals("help"))
        {
            System.out.println();
            System.out.println("\tstatus             \t\tShows the status of flowseer");
            System.out.println("\tstart              \t\tStarts flowseer");
            System.out.println("\tstop               \t\tStops flowseer");
            System.out.println("\tshow-flow-data-list\t\tShows the flow data list table");
            System.out.println();
        }
        else if (cmd.equals("status"))
        {
            if (fwdService.isFlowSeerActive)
            {
                System.out.println("Flowseer is active.");
            }
            else
            {
                System.out.println("Flowseer is not active.");
            }
        }
        else if (cmd.equals("start"))
        {
            if (!fwdService.isFlowSeerActive)
            {
                int k,b,t;
                System.out.println("Starting FlowSeer...");
                System.out.println("Enter no of packets to sample: ");
                k = readInt();

                if (k == Integer.MIN_VALUE)
                {
                    // some error occured
                    // no need to do anything
                }
                else if (k < 1)
                {
                    System.out.println("Please enter a positive number!");
                    System.out.println("Please try again.");
                }
                else
                {
                    System.out.println("Enter bandwidth limit (Mbits/s): ");
                    b = readInt();

                    if (b < 1 && b > Integer.MIN_VALUE)
                    {
                        System.out.println("Please enter a positive number!");
                        System.out.println("Please try again.");
                    }
                    else
                    {
                        System.out.println("Enter time threshold (secs): ");
                        t = readInt();

                        if (t < 1 && t > Integer.MIN_VALUE)
                        {
                            System.out.println("Please enter a positive number!");
                            System.out.println("Please try again.");
                        }
                        else
                        {
                            System.out.println("Enter host address / IP address of classifier: ");
                            String host = readString();
                            System.out.println("Enter port no for training: ");
                            int trainingPort = readInt();

                            while (trainingPort < 1024 || trainingPort > 65535)
                            {
                                System.out.println("Error: Please enter a port no between 1024 and 65535");
                                System.out.println("Enter port no for training: ");
                                trainingPort = readInt();
                            }

                            System.out.println("Enter port no for testing: ");
                            int testingPort = readInt();

                            while (testingPort < 1024 || testingPort > 65535)
                            {
                                System.out.println("Error: Please enter a port no between 1024 and 65535");
                                System.out.println("Enter port no for testing: ");
                                testingPort = readInt();
                            }

                            if (fwdService.startFlowSeer(k, b, t, host, trainingPort, testingPort))
                            {
                                System.out.println("Started FlowSeer");
                            }
                            else
                            {
                                System.out.println("Could not start FlowSeer");
                            }
                        }
                    }
                }
            }
            else
            {
                System.out.println("FlowSeer is already active.");
            }

        }
        else if (cmd.equals("stop"))
        {
            if (fwdService.isFlowSeerActive)
            {
                System.out.println("Stopping FlowSeer...");
                fwdService.stopFlowSeer();
                System.out.println("Stopped FlowSeer");
            }
            else
            {
                System.out.println("FlowSeer is already stopped.");
            }

        }
        else if(cmd.equals("show-flow-data-list"))
        {
            if (fwdService.isFlowSeerActive)
            {
                flowDataList.print(System.out);
            }
            else
            {
                System.out.println("FlowSeer is not active.");
                System.out.println("Type \"flowseer start\" to start FlowSeer.");
            }
        }
        else
        {
            System.out.println("Command not found.");
            System.out.println("Type \"flowseer help\" to view available options.");
        }
    }

    protected String readString()
    {
        int i;
        char c;
        StringBuffer sb = new StringBuffer();
        int len = 0;
        int cursor = 0;
        try
        {
            do
            {
                i = System.in.read();
                c = (char)i;

                // modifying the string buffer
                if (i == 127 && cursor > 0)
                {
                    // backspace
                    // if cursor is at the end of input
                    if (cursor == len)
                    {
                        sb.setCharAt(sb.length()-1, ' ');
                        //len--;
                        cursor--;
                    }
                    // else the cursor is in the middle of the input
                    else
                    {
                        sb.deleteCharAt(cursor - 1);
                        sb.append(' ');
                        //len--;
                        cursor--;
                    }
                }
                else if (i>=32 && i < 126)
                {
                    // normal character
                    // if cursor is at the end of the input
                    if (cursor == len)
                    {
                        sb.append(c);
                        len++;
                        cursor++;
                    }
                    // else the cursor is in between the input
                    else
                    {
                        sb.insert(cursor, c);
                        len++;
                        cursor++;
                    }
                }
                else if (i == 13)
                {
                    // enter
                }
                else if (i == 27)
                {
                    i = System.in.read();
                    c = (char)i;

                    if (i == 91)
                    {
                        i = System.in.read();
                        c = (char)i;

                        if (i == 51)
                        {
                            i = System.in.read();
                            c = (char)i;

                            if (i == 126 && cursor < len)
                            {
                                // delete
                                sb.deleteCharAt(cursor);
                                sb.append(' ');
                            }
                        }
                        else if (i == 68 && cursor > 0)
                        {
                            // left
                            cursor--;
                        }
                        else if (i == 67 && cursor < len)
                        {
                            // right
                            cursor++;
                        }
                        else if (i == 65)
                        {
                            // up
                            // do nothing
                        }
                        else if (i == 66)
                        {
                            // down
                            // do nothing
                        }
                    }
                    else if (i == 79)
                    {
                        i = System.in.read();
                        c = (char)i;

                        if (i == 72)
                        {
                            // home
                            cursor = 0;
                        }
                        else if (i == 70)
                        {
                            // end
                            cursor = len;
                        }
                    }
                }

                // printing the stringbuffer onto the screen
                System.out.print("\r");
                System.out.print(sb.toString());
                System.out.print("\r");
                for (int j=0; j<cursor; j++)
                {
                    System.out.print(sb.charAt(j));
                }

                // updating the string buffer
                sb = new StringBuffer(sb.toString().trim());
                len = sb.length();

            }while(i != 13);
        }
        catch (InterruptedIOException ex)
        {

        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        System.out.println();
        return sb.toString();
    }

    protected int readInt()
    {
        String s;
        int n;

        try
        {
            s = readString();
            n = Integer.parseInt(s);
        }
        catch (NumberFormatException ex)
        {
            System.out.println("Error: Please enter a number");
            n = Integer.MIN_VALUE;
        }

        return n;
    }
}
