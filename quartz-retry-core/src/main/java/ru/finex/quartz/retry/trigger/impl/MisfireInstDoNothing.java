/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.finex.quartz.retry.trigger.impl;

import java.util.Date;
import org.quartz.Calendar;
import org.quartz.spi.OperableTrigger;

/**
 *
 * @author HOME
 */
public class MisfireInstDoNothing extends MisfireInstruction implements MisfireHandler{

    @Override
    public void getMisfireBehavior(RetryCronScheduleBuilder cb){
        cb.withMisfireHandlingInstructionDoNothing();            
    };

    @Override
    public void handleMisfire(OperableTrigger trigger, Calendar cal) {
        Date newFireTime = trigger.getFireTimeAfter(new Date());
        while (newFireTime != null && cal != null && !cal.isTimeIncluded(newFireTime.getTime())) {
            newFireTime = trigger.getFireTimeAfter(newFireTime);
        }
        trigger.setNextFireTime(newFireTime);
    }
    
}
