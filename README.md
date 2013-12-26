A lightweight MQTT server
=========================
A lightweight MQTT server that will keep running for the duration of your Android application using the Paho Java MQTT Client. 

Use LMAX for message queue.

Use MapDB to store in flight messages.

Use Netty for IO (based on moquette-mqtt writen by andrea).

Feature
----
* QoS 0,1,2 supported
* Custom authentication
* Custom subscription store
* Custom off line message store
* Custom in flight store
* Broker bridge

Simple Example
----
```java
// TODO 
```


License
-------

    Copyright 2013 Shimon xin
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.