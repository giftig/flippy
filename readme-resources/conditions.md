# Condition configuration

This explains the structure of various types of conditions offered out of the box with flippy
and their JSON representation.

## Matchers

### Global on/off

The simplest conditions: always true or always false

    {
      "condition_type": "true"
    }

    {
      "condition_type": "false"
    }

### Equals

Just checks if the value equals a specific value:

    {
      "condition_type": "equals",
      "value": "Ms. Cloud"
    }

    {
      "condition_type": "equals",
      "value": 1337
    }

### Proportion

A proportion rule will be true for approximately the proportion of possible values
configured. It does this in a consistent / predictable way, i.e. a value of X will
always yield the same result. Specifically, it takes the string value of the context
component being matched, hashes it with SHA-1, and uses the first half of that hash
to determine its position on a number line. As a result, the distribution should be
relatively uniform.

It's worth noting that this will work better with more diverse data, as the larger the
set of input data, the greater the resolution of the resulting distribution. IP address,
email address, user agent, or other unique or semi-unique user identifiers would be
ideal data points for the proportion condition.

Proportion is provided as a float between 0 and 1.

    {
      "condition_type": "proportion",
      "proportion": 0.8
    }

### String matchers
#### Range

Checks if the string is between two values alphabetically:

    {
      "condition_type": "string:range",
      "low": "abacus",
      "high: "zebra"
    }

#### Regex

Checks if the string matches a given regular expression:

    {
      "condition_type": "string:regex",
      "pattern": "^[A-Za-z0-9_]+$"
    }

#### Substring

Checks if the string contains a specific substring:

    {
      "condition_type": "string:substring",
      "value": "Mr. "
    }

#### One of

Checks if the string is one of the provided options. This is a less verbose
alternative to using an OR and multiple EQUALS rules:

    {
      "condition_type": "string:oneof",
      "options": ["opt1", "opt2", "opt3"]
    }

### Networking
#### IPv4 range

Checks if a provided IP address is in the given IP range, provided in the
aaa.bbb.ccc.ddd/nn range form. Currently only IPv4 is supported.

    {
      "condition_type": "networking:iprange",
      "range": "192.168.1.1/16"
    }

## Not

Inverts the wrapped condition, eg. value should not be "error":

    {
      "condition_type": "not",
      "condition": {
        "condition_type": "equals",
        "value": "error"
      }
    }

## And / or

Combine a list of conditions with AND or OR logic. No limit on the number of conditions
to combine. eg. value must be "Tifa" or "Cloud":

    {
      "condition_type": "or",
      "conditions": [
        {
          "condition_type": "equals",
          "value": "Tifa"
        },
        {
          "condition_type": "equals",
          "value": "Cloud"
        }
      ]
    }

... or must be neither:

    {
      "condition_type": "and",
      "conditions": [
        {
          "condition_type": "not",
          "condition": {
            "condition_type": "equals",
            "value": "Tifa"
          }
        },
        {
          "condition_type": "not",
          "condition": {
            "condition_type": "equals",
            "value": "Cloud"
          }
        }
      ]
    }

## Field selection (namespacing)

Namespacing, or field selection, allows you to check a field in a set of data - since switches
are compared against data maps, you'll generally need a namespacing wrapper around any switch
condition you use.

For example, to check if your user's name is "red\_xiii":

    {
      "condition_type": "namespaced",
      "attr": "username",
      "condition": {
        "condition_type": "equals",
        "value": "red_xiii"
      }
    }

By default, if the data provided when checking a switch doesn't contain the desired field, the
condition will be assumed to be false. You can change this behaviour by providing
```fallback = true``` to the namespace wrapper. For example, if you want to decide switch state
based on a new parameter which you don't always provide, but turn the switch on if it's absent:

    {
      "condition_type": "namespaced",
      "attr": "field_may_be_missing",
      "fallback": true,
      "condition": {
        "condition_type": "equals",
        "value": "if my field is present it must be this value!"
      }
    }
