
# WakeGuard - Intelligent IoT-Based Alarm App

## Project Motivation

Waking up early in the morning is a challenge faced by many individuals, often leading to repeated reliance on alarm clocks that fail to achieve the desired result. Many people find themselves in a continuous struggle to wake up on time, only to turn off their alarms and fall back asleep, thereby missing out on a productive start to the day. This is a widespread problem, especially in today’s fast-paced world, where maintaining a consistent morning routine is crucial for success.

The solution needed to be more than just a traditional alarm clock that rings at a set time. It required a smarter approach that could monitor whether someone is still in bed and ensure they stay awake. The idea was to develop a device that not only detects when a person is still sleeping but also triggers an alarm that continues until they have fully left the bed. Furthermore, to prevent the user from returning to sleep after waking up, the system needed to re-trigger the alarm if it detected a return to bed within a specified monitoring period.

This led to the creation of an IoT device, paired with a mobile application, capable of monitoring bed pressure and ensuring that the user remains awake. This project addresses a common challenge faced by many and showcases the practical application of IoT technology and mobile app development in solving real-world problems.

## WakeGuard App Description

**WakeGuard** is an innovative Android application designed to help users overcome the common struggle of waking up on time and staying awake. The app works in conjunction with two IoT modules: a **Bed Sensor** and a **Monitoring System**. These components are strategically placed in the user's bedroom to ensure they get out of bed and stay out.

### Components

- **Bed Sensor**: This sensor attaches to the legs of the bed and is responsible for detecting the weight on the bed. It starts monitoring three hours before the set alarm time and continues until the alarm duration ends.

- **Monitoring System**: This module can be discreetly installed on a high ceiling or behind a piece of furniture, making it difficult to access and disable. When the bed sensor detects that the user is still in bed at the start of the alarm, it signals the monitoring system to trigger a persistent alarm.

### Key Features

- **Customizable Alarm Duration**: Users can set a start time and an end time for the alarm. The system ensures that the user does not fall back asleep during this period.
- **Proactive Monitoring**: Three hours before the alarm, the bed sensor begins recording the weight on the bed, preparing the system to detect when the user remains in bed.
- **Intelligent Alarm System**: If the bed sensor detects weight on the bed at the alarm start time, the monitoring system triggers the alarm. The alarm will continue until the bed is vacated.
- **Reactivation Feature**: Even after getting out of bed, if the user lies down again during the alarm duration, the bed sensor reactivates the monitoring system, causing the alarm to ring again until the user gets out of bed.

### Availability

The WakeGuard app is currently available exclusively for Android devices.

## Parts Used

The WakeGuard project utilizes the following components to create a reliable and effective IoT-based alarm system:

- **Wemos D1 Mini - IoT ESP8266 Based Development Board (x2)**: These compact and powerful microcontroller boards form the core of the WakeGuard system, handling the communication between the bed sensor and the monitoring system.
- **50kg Half-bridge Experiments Body Scale Load Cell Sensor**: This sensor is used to measure the weight on the bed, allowing the system to detect whether the user is still lying down.
- **Active Buzzer Module**: This component is responsible for generating the alarm sound when triggered by the monitoring system.
- **HX711 Amplifier**: This amplifier is used in conjunction with the load cell sensor to accurately measure and transmit weight data to the Wemos D1 Mini.

