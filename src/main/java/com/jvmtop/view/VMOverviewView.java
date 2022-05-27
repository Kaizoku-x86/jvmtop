/**
 * jvmtop - java monitoring for the command-line
 *
 * Copyright (C) 2013 by Patric Rufflar. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.jvmtop.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jvmtop.monitor.VMInfo;
import com.jvmtop.monitor.VMInfoState;
import com.jvmtop.openjdk.tools.LocalVirtualMachine;
import java.util.Date;

/**
 * "overview" view, providing the most-important metrics of all accessible jvms in a top-like manner.
 *
 * @author paru
 *
 */
public class VMOverviewView extends AbstractConsoleView
{
  private boolean firstTime = true;
  
  private int vmid = 0;

  private List<VMInfo>                      vmInfoList = new ArrayList<VMInfo>();

  private Map<Integer, LocalVirtualMachine> vmMap      = new HashMap<Integer, LocalVirtualMachine>();
  
  public VMOverviewView(Integer width) {
    super(width);
  }
  
  public VMOverviewView(Integer width, Integer vmid) throws Exception {
    super(width);
    this.vmid = vmid;
    LocalVirtualMachine localVirtualMachine = LocalVirtualMachine
        .getLocalVirtualMachine(vmid);
  }

  public void printView() throws Exception
  {
    if(firstTime){
        printHeader();
        firstTime = false;
    }

    //to reduce cpu effort, scan only every 5 iterations for new vms
    scanForNewVMs();

    updateVMs(vmInfoList);

    Collections.sort(vmInfoList, VMInfo.CPU_LOAD_COMPARATOR);

    for (VMInfo vmInfo : vmInfoList)
    {
      if (vmInfo.getState() == VMInfoState.ATTACHED
)
      {
        printVM(vmInfo);
      }
      else if (vmInfo.getState() == VMInfoState.ATTACHED_UPDATE_ERROR)
      {
        System.out
            .printf(
                "%5d %-15.15s [ERROR: Could not fetch telemetries (Process DEAD?)] %n",
                vmInfo.getId(), getEntryPointClass(vmInfo.getDisplayName()));
        System.exit(1);

      }
      else if (vmInfo.getState() == VMInfoState.ERROR_DURING_ATTACH)
      {
        System.out.printf("%5d %-15.15s [ERROR: Could not attach to VM] %n",
            vmInfo.getId(), getEntryPointClass(vmInfo.getDisplayName()));
      }
      else if (vmInfo.getState() == VMInfoState.CONNECTION_REFUSED)
      {
        System.out.printf(
            "%5d %-15.15s [ERROR: Connection refused/access denied] %n",
            vmInfo.getId(), getEntryPointClass(vmInfo.getDisplayName()));
      }

    }
  }

  /**
   * @param name
   * @return
   */
  private String getEntryPointClass(String name)
  {
    if (name.indexOf(' ') > 0)
    {
      name = name.substring(0, name.indexOf(' '));
    }
    return rightStr(name, 15);
  }

  /**
   * @param localvm
   * @param vmid
   * @param vmInfo
   * @return
   * @throws Exception
   */
  private void printVM(VMInfo vmInfo) throws Exception
  {
    if(vmid != 0 && vmInfo.getId()==vmid){
        String deadlockState = "";
        if (vmInfo.hasDeadlockThreads())
        {
          deadlockState = "!D";
        }

        System.out.println(
                new Date() +", "+
                vmInfo.getId() +", "+
                getEntryPointClass(vmInfo.getDisplayName())+", "+
                toMB(vmInfo.getHeapUsed())+", "+
                toMB(vmInfo.getHeapMax())+", "+
                toMB(vmInfo.getNonHeapUsed())+", "+
                toMB(vmInfo.getNonHeapMax())+", "+
                vmInfo.getCpuLoad() * 100+", "+
                vmInfo.getGcLoad() * 100+", "+
                vmInfo.getVMVersion()+", "+
                vmInfo.getOSUser()+", "+
                vmInfo.getThreadCount()+", "+
                deadlockState);
    } else if(vmid == 0){
        String deadlockState = "";
        if (vmInfo.hasDeadlockThreads())
        {
          deadlockState = "!D";
        }

        System.out.println(
                new Date() +", "+
                vmInfo.getId() +", "+
                getEntryPointClass(vmInfo.getDisplayName())+", "+
                toMB(vmInfo.getHeapUsed())+", "+
                toMB(vmInfo.getHeapMax())+", "+
                toMB(vmInfo.getNonHeapUsed())+", "+
                toMB(vmInfo.getNonHeapMax())+", "+
                vmInfo.getCpuLoad() * 100+", "+
                vmInfo.getGcLoad() * 100+", "+
                vmInfo.getVMVersion()+", "+
                vmInfo.getOSUser()+", "+
                vmInfo.getThreadCount()+", "+
                deadlockState);
    }

  }

  /**
   * @param vmList
   * @throws Exception
   */
  private void updateVMs(List<VMInfo> vmList) throws Exception
  {
    for (VMInfo vmInfo : vmList)
    {
      vmInfo.update();
    }
  }

  /**
   * @param vmMap
   * @param vmMap
   * @param set
   */
  private void scanForNewVMs()
  {
    Map<Integer, LocalVirtualMachine> machines = LocalVirtualMachine
        .getNewVirtualMachines(vmMap);
    Set<Entry<Integer, LocalVirtualMachine>> set = machines.entrySet();

    for (Entry<Integer, LocalVirtualMachine> entry : set)
    {
      LocalVirtualMachine localvm = entry.getValue();
      int vmid = localvm.vmid();

      if (!vmMap.containsKey(vmid))
      {
        VMInfo vmInfo = VMInfo.processNewVM(localvm, vmid);
        vmInfoList.add(vmInfo);
      }
    }
    vmMap = machines;
  }

  /**
  *
  */
  private void printHeader()
  {
    System.out.println(
        "Date, PID, MAIN-CLASS, HPCUR, HPMAX, NHCUR, NHMAX, CPU, GC, "
                + "VM, USERNAME, #T, DL");
  }
}
