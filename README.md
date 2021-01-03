# FonGuard

<p align="center">
<img src="fonguard.png" width="200" height="200" alt="FonGuard logo" /><br>
<strong><i>FonGuard</i> turns your phone into a sentinel watching out for intruders<br> and
reporting any suspect motion, noise or vibration<br>in real time.</strong>
</p>

## Intrusion detection (Triggers)
FonGuard aims to detect intruders by using any of these three **triggers**:
* Camera (motion detection)
* Microphone (noise detection)
* Accelerometer (vibration detection)

## Intrusion reporting (Actions)
Once FonGuard has detected something, it will perform the **action(s)** associated with the trigger
as described in the **rules** ([see below](#rules)).

The following actions are currently available:
* SMS
* MMS
* Phone call
* HTTP request
* AWS S3 upload

:warning: It is very important to understand that different actions have **different network
requirements**. Depending on the network requirements and the network(s) available, an action might
fail or succeed.

| Action            | Mobile Network | Mobile Data | Wi-Fi      |
|-------------------|----------------|-------------|------------|
| **SMS**           | Required       |             |            |
| **MMS**           |                | Required    |            |
| **Phone call**    | Required       |             |            |
| **HTTP**          |                | Sufficient  | Sufficient |
| **AWS S3**        |                | Sufficient  | Sufficient |

For example:
* Sending an MMS requires Mobile Data to be enabled and available, while sending an SMS only
requires Mobile Network and will work even if you have no Mobile Data (left)
* Sending an HTTP request can work with **either** Mobile Data or Wi-Fi Internet alone

For this reason, it is recommended to **have multiple actions configured**. For example:
* MMS: will send a notification with attachment
* SMS: will send a notification (without attachment) in case you have no Mobile Data left
* AWS S3: will upload the attachment in case there is no Mobile Data left for the MMS but you still
have Internet access (e.g. via Wi-Fi)

## Rules
Rules are simply the **link** between a **trigger** (e.g. motion) and an **action** (e.g. upload the
picture where the motion was detected on Google Drive).

Additionally, rules hold extra parameters to tweak the behavior of the action
([see below](#example-configuration)).

An unlimited number of rules can be created for any combination of trigger and action.

## Settings

Settings are at the core of FonGuard and need to be carefully tweaked for the app to work correctly.

Each device is different and so **the triggers settings need to be adjusted for the specific
hardware** that you have. For example, some cameras might be much more noisy than others, so in this
case you would need to configure a higher `pixel_value_diff_threshold`.

Also, by default, there is no action defined, so you need **to define the action(s) you want and
create rule(s) to associate trigger(s) with the action(s) you defined**.

All this can be done easily **by writing a JSON file** and then **importing it into the app**
("*Import*" tab in the app). Due to the vast (and growing) number of settings in FonGuard, it is NOT
possible to edit the settings via the user interface.

### Example configuration
Here is an example JSON settings file along with the description of each parameter. You can
copy/paste it and modify the values to suit your needs. **Make sure to remove all the comments
(everything after `//`, including `//`) because otherwise you will get syntax errors**.

```jsonc
{
    "triggers": {
        "motion": {
            "camera_id": "0", // ID of the camera to use for motion detection. Can be retrieved in the list of cameras in the "Motion" tab of the app.
            "pixel_value_diff_threshold": 10, // [1;255] Minimum difference between two pixels at the same position in two consecutive images to consider them different.
            "pixel_number_diff_threshold": 10, // [1;+inf] Minimum number of different pixels between two consecutive images to consider there is a motion.
        }
    },
    "actions": {
        "phone_sms": [
            {
                "id": "send-to-iphone", // Unique string ID of the action used for logging and referencing the action in the rules section.
                "recipient_phone_number": "+33712345678" // Phone number of the recipient (both international and local phone number formats work).
            }
        ],
        "phone_mms": [
            {
                "id": "send-to-iphone", // Unique string ID of the action used for logging and referencing the action in the rules section.
                "recipient_phone_number": "+33712345678" // Phone number of the recipient (both international and local phone number formats work).
            }
        ],
        "phone_call": [
            {
                "id": "call-iphone", // Unique string ID of the action used for logging and referencing the action in the rules section.
                "recipient_phone_number": "+33712345678" // Phone number of the recipient (both international and local phone number formats work).
            }
        ],
        "http": [
            {
                "id": "http-action-1", // Unique string ID of the action used for logging and referencing the action in the rules section.
                "method": "POST", // GET|POST HTTP method to use for this action. Only GET and POST are supported. Attachments (e.g. pictures for motion trigger) can only be sent with POST method. Use GET for notification-only.
                "url": "https://www.example.com/fonguard-test", // Target URL.
                "headers": [ // List of HTTP headers to add to the request.
                    {
                        "name": "Authentication", // Name of the HTTP header.
                        "value": "Basic Rm9uR3VhcmQ6Rm9uR3VhcmQ=" // Value of the HTTP header.
                    }
                ]
            }
        ],
        "aws_s3": [
            {
                "id": "upload-to-my-s3-bucket", // Unique string ID of the action used for logging and referencing the action in the rules section.
                "aws_region": "eu-west-1", // AWS region in which the bucket was created.
                "aws_access_key_id": "AKIA...", // AWS access key ID with s3:PutObject permission.
                "aws_secret_access_key": "...", // AWS secret access key associated with the access key ID above.
                "bucket_name": "fonguard", // Name of the bucket in which to upload the attachment (e.g. picture for motion trigger).
                "key_prefix": "img" // Prefix to use for naming the files in the bucket. The full filepath will be {PREFIX}{DATETIME} where {DATETIME} is yyyy-MM-dd'T'HH:mm:ss.SSSZ.
            }
        ]
    },
    "rules": [
        {
            "id": "rule1", // Unique string ID of the rule used for logging and internal needs.
            "trigger": "motion", // Type of trigger, only "motion" is supported at this time.
            "action": "aws_s3:upload-to-my-s3-bucket", // Action to perform when the trigger is fired. Format is ACTION_TYPE:ACTION_ID.
            "include_payload": true, // Whether to include the payload (i.e. picture for motion trigger) in the action or not.
            "cooldown_ms": 1000, // [0;+inf] Minimum delay in milliseconds between two actions can be triggered by the same rule. This avoids to trigger an action 30+ times in a single second when a motion is happening for example.
            "retries": 3, // [0;+inf] Maximum number of times to retry an action if it fails. 0 means don't retry if an action fails.
            "retry_delay_ms": 5000 // [0;+inf] Time to wait in milliseconds before retrying an action if it fails. 0 means retry immediately when an action fails.
        }
    ]
}
```

## Roadmap/Feature list
Checked checkbox means the feature is already implemented and working, unchecked checkbox means it's
still in development.

* Triggers
    - [x] Camera (motion detection)
    - [ ] Microphone (noise detection)
    - [ ] Accelerometer (vibration detection)
    - [ ] Heartbeat (device/app death detection)
* Actions
    - [x] SMS
    - [x] MMS
    - [x] Phone call
    - [x] HTTP (GET, POST)
    - [x] AWS S3
* Rules
    - [x] Retry mechanism
    - [x] Cooldown mechanism
    - [x] Enable/disable payload attachment
* Settings
    - [x] Import
    - [x] Export

## Contributing
Any help is very appreciated and most welcome!

Feel free to contribute to the project by:
* Implementing a new feature or fixing a bug:
[open a Pull Request](https://github.com/GuiTeK/FonGuard/pulls)
* Reporting a bug or suggesting a new feature:
[open an Issue](https://github.com/GuiTeK/FonGuard/issues)

### Contributors
* [GuiTeK](https://github.com/GuiTeK) (project owner)
