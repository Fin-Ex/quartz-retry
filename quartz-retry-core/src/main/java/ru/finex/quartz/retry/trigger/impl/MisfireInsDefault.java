/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.finex.quartz.retry.trigger.impl;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author HOME
 */
@Slf4j
public class MisfireInsDefault extends MisfireInstruction{
    private int misfireInstruction;

    public MisfireInsDefault(int misfireInstruction) {
        this.misfireInstruction = misfireInstruction;
    }
    
    @Override
    public void getMisfireBehavior(RetryCronScheduleBuilder cb) {
        log.warn("Unrecognized misfire policy {}. Derived builder will use the default cron " +
                    "trigger behavior (MISFIRE_INSTRUCTION_FIRE_ONCE_NOW)", misfireInstruction);
    }
    
}
