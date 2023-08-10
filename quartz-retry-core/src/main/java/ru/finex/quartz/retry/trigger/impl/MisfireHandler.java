/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ru.finex.quartz.retry.trigger.impl;

import org.quartz.spi.OperableTrigger;

/**
 *
 * @author HOME
 */
public interface MisfireHandler {
    void handleMisfire(OperableTrigger trigger, org.quartz.Calendar cal);

}
