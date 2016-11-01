(function($) {
  var Widgets = {};

  // Pretty names. Default to the same name if not present in this mapping
  var conditionAliases = {
    one_of: 'one of',
    ip_range: 'IPv4 range',
    multiple: 'and/or',
    namespaced: 'field must match...',
    proportion: 'percentage',
    raw: '(edit as JSON)'
  };

  // Map of official serialisation names to internal class names
  var conditionTypes = {
    and: 'multiple',
    'false': 'off',
    'networking:iprange': 'ip_range',
    or: 'multiple',
    'string:regex': 'regex',
    'string:oneof': 'one_of',
    'string:substring': 'substring',
    'true': 'on'
  };

  /**
   * Create a named condition type, defaulting to raw if the type isn't
   * understood. If the type is raw and we're replacing another widget,
   * initialise the raw data with the JSON value of that widget.
   */
  var createCondition = function(name, replacing) {
    var t = conditionTypes[name] || name;
    t = Widgets[t] ? t : 'raw';
    var Condition = Widgets[t];
    var c = new Condition();

    if (t === 'raw' && replacing && replacing.clean()) {
      c.init(replacing.buildJSON());
    }
    return c;
  };

  var baseConditions = ['namespaced', 'multiple', 'not', 'on', 'off', 'raw'];
  var subConditions = [
    'equals',
    'namespaced',
    'multiple',
    'not',
    'regex',
    'substring',
    'one_of',
    'ip_range',
    'proportion',
    'raw'
  ];

  // Convenience function for generating option lists from available conditions
  var generateConditionList = function(conditions, defaultLabel) {
    defaultLabel = defaultLabel || 'Change condition';

    var $conditions = $('<select>');
    $conditions.append($('<option>').val('').text(defaultLabel));

    for (var i = 0; i < conditions.length; i++) {
      var cond = conditions[i];
      var $opt = $('<option>').val(cond).text(conditionAliases[cond] || cond);
      $conditions.append($opt);
    }
    return $conditions;
  };
  baseConditions.asOptions = function(defaultLabel) {
    return generateConditionList(baseConditions, defaultLabel);
  };
  subConditions.asOptions = function(defaultLabel) {
    return generateConditionList(subConditions, defaultLabel);
  };

  /*
   * All widgets must define:
   *
   * init:        initialise the widget's form with data provided
   * renderForm:  render a form to help configure the widget
   * buildJSON:   build javascript objects representing the JSON structure
   * clean:       pull in completed form data and verify it; return whether it's valid
   */
  Widgets.namespaced = function() {
    var self = this;

    self.field = null;
    self.condition = null;

    self.init = function(data) {
      self.renderForm();

      var c = createCondition(data.condition.condition_type);
      self.condition = c;

      self.$form.children('[name="field"]').val(data.attr);
      self.$form.children('[name="fallback"]').val(data.fallback + '');
      self.$form.children('[name="condition"]').val(
        conditionTypes[data.condition.condition_type] || data.condition.condition_type
      );
      self.$form.append(c.init(data.condition).addClass('subcondition'));

      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg namespaced');
      var $field = $('<input>').attr('name', 'field');
      var $conditions = subConditions.asOptions('Select condition').attr('name', 'condition');
      var $fallback = $('<select>').attr('name', 'fallback').addClass('fallback').append(
        $('<option>').val('').text('Select fallback'),
        $('<option>').val('true').text('true'),
        $('<option>').val('false').text('false')
      );


      $conditions.on('change', function() {
        var condName = $(this).val();
        var $oldSubform = $form.children('.subcondition');

        if (!condName) {
          $oldSubform.remove();
          self.condition = null;
          return;
        }

        var sub = createCondition(condName, self.condition);
        self.condition = sub;
        var $subform = sub.renderForm().addClass('subcondition');

        if ($oldSubform.length === 0) {
          $form.append($subform);
        } else {
          $oldSubform.replaceWith($subform);
        }
      });

      $form.html($field).append($conditions).append($fallback);

      $form.addSubmit = function($submit) {
        $conditions.after($submit);
      };

      // Labellingjs parse JSON
      $field.before('The field ');
      $field.after(' ... ');
      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      self.field = self.$form.children('[name="field"]').val();
      self.fallback = self.$form.children('[name="fallback"]').val() === 'true';

      if (!self.field) {
        self.error = 'No field specified for namespaced condition';
        return false;
      }
      if (!self.condition) {
        self.error = 'No condition selected for namespacing on field ' + self.field;
        return false;
      }
      if(!self.condition.clean()) {
        self.error = self.condition.error;
        return false;
      }

      return true;
    };

    self.buildJSON = function() {
      return {
        condition_type: 'namespaced',
        attr: self.field,
        condition: self.condition.buildJSON(),
        fallback: self.fallback
      }
    };
  };

  Widgets.not = function() {
    var self = this;
    var condition = null;

    self.init = function(data) {
      self.renderForm();

      var c = createCondition(data.condition.condition_type);
      self.condition = c;
      self.$form.append(c.init(data.condition).addClass('subcondition'));
      self.$form.children('[name="condition"]').val(
        conditionTypes[data.condition.condition_type] || data.condition.condition_type
      );

      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg not');
      var $conditions = subConditions.asOptions('Select condition').attr('name', 'condition');

      $conditions.on('change', function() {
        var condName = $(this).val();
        var $oldSubform = $form.children('.subcondition');

        if (!condName) {
          $oldSubform.remove();
          self.condition = null;
          return;
        }

        var sub = createCondition(condName, self.condition);
        self.condition = sub;
        var $subform = sub.renderForm().addClass('subcondition');

        if ($oldSubform.length === 0) {
          $form.append($subform);
        } else {
          $oldSubform.replaceWith($subform);
        }
      });

      $form.html($conditions);

      // Labelling
      $conditions.before('NOT ');

      $form.addSubmit = function($submit) {
        $conditions.after($submit);
      };

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      if (!self.condition) {
        self.error = 'No condition selected on "NOT"';
        return false;
      }
      if(!self.condition.clean()) {
        self.error = self.condition.error;
        return false;
      }
      return true;
    };

    self.buildJSON = function() {
      return {
        condition_type: 'not',
        condition: self.condition.buildJSON()
      };
    };
  };

  Widgets.equals = function() {
    var self = this;

    self.init = function(data) {
      self.renderForm();

      var value;
      if (data.value === null) {
        value = 'null';
      } else {
        value = data.value;
      }

      self.$form.children('[name="value"]').val(value);
      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg equals');
      var $field = $('<input>').attr('name', 'value');
      $form.append($field);

      // Labelling
      $field.before('...must equal ');

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      var value = self.$form.children('[name="value"]').val();

      // Handle data types
      if (value.match(/^\d+\.\d+$/)) {
        self.value = parseFloat(value);
      } else if (value.match(/^\d+$/)) {
        self.value = parseInt(value);
      } else if (value === 'true' || value === 'false') {
        self.value = value === 'true';
      } else if (value === 'null') {
        self.value = null;
      } else {
        self.value = value;
      }

      return true;
    };
    self.buildJSON = function() {
      return {condition_type: 'equals', value: self.value};
    };
  };

  Widgets.on = function() {
    var self = this;

    self.init = function(data) {
      self.renderForm();
      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg always-on');
      $form.html($('<span>').addClass('on-off').text('Always on'));

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      return true;
    };

    self.buildJSON = function() {
      return {condition_type: 'true'};
    };
  };
  Widgets.off = function() {
    var self = this;

    self.init = function(data) {
      self.renderForm();
      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg always-off');
      $form.html($('<span>').addClass('on-off').text('Always off'));

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      return true;
    };

    self.buildJSON = function() {
      return {condition_type: 'false'};
    };
  };

  // For and/or condition lists
  Widgets.multiple = function() {
    var self = this;
    self.conditions = [];
    self.conditionType = null;
    var nextIndex = 0;

    self.init = function(data) {
      self.renderForm();

      self.$form.children('[name="type"]').val(data.condition_type);

      for (var i = 0; i < data.conditions.length; i++) {
        var c = createCondition(data.conditions[i].condition_type);

        self.$form.children('.stage').append(c.init(data.conditions[i]));
        self.conditions.push(c);
      }

      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg multi');
      var $type = $('<select>').attr('name', 'type').append(
        $('<option>').text('all').val('and'),
        $('<option>').text('one').val('or')
      );
      var $stagingArea = $('<div>').addClass('stage');
      var $another = subConditions.asOptions('Add condition').on('change', function() {
        var condType = $(this).val();
        if (!condType) {
          return;
        }
        var cond = createCondition(condType);
        cond.index = nextIndex++;
        self.conditions.push(cond);

        var $cond = cond.renderForm();
        $cond.append($('<button>').attr('type', 'button').addClass('cancel').text('Remove').click(function() {
          self.conditions = self.conditions.filter(function(c) {
            return c.index !== cond.index;
          });
          $cond.remove();
        }));

        $stagingArea.append($cond);
        $another.val('');
      });

      $form.html($type);
      $type.after(' of the following must be true:');
      $form.append($another);
      $form.append($stagingArea);

      self.$form = $form;
      $form.addSubmit = function($submit) {
        $another.after($submit);
      };
      return $form;
    };

    self.clean = function() {
      if (self.conditions.length === 0) {
        self.error = 'No conditions selected for and/or';
        return false;
      }
      self.conditionType = self.$form.children('[name="type"]').val();

      var errors = [];

      for (var i = 0; i < self.conditions.length; i++) {
        if (!self.conditions[i].clean()) {
          errors.push(self.conditions[i].error);
        }
      }

      if (errors.length !== 0) {
        self.error = errors.join('<br>');
      }

      return true;
    };

    self.buildJSON = function() {
      return {
        condition_type: self.conditionType,
        conditions: self.conditions.map(function(c) {
          return c.buildJSON();
        })
      };
    };
  };

  Widgets.substring = function() {
    var self = this;
    self.value = null;

    self.init = function(data) {
      self.renderForm();
      self.$form.children('[name="value"]').val(data.value);
      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg substring');
      var $value = $('<input>').attr('name', 'value');
      $form.html($value);

      // Labelling
      $value.before('...must contain substring ');

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      self.value = self.$form.children('[name="value"]').val();
      return true;
    };
    self.buildJSON = function() {
      return {condition_type: 'string:substring', value: self.value};
    };
  };
  Widgets.regex = function() {
    var self = this;
    self.pattern = null;

    self.init = function(data) {
      self.renderForm();

      self.$form.children('[name="pattern"]').val(data.pattern);
      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg regex');
      var $pattern = $('<input>').attr('name', 'pattern');
      $form.html($pattern);

      // Labelling
      $pattern.before('...must match regular expression ');

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      self.pattern = self.$form.children('[name="pattern"]').val();
      return true;
    };
    self.buildJSON = function() {
      return {condition_type: 'string:regex', pattern: self.pattern};
    };
  };

  Widgets.one_of = function() {
    var self = this;
    self.options = [];

    var handleFields = function(removeEmpty) {
      var $values = self.$values.children('.value');

      for (var i = 0; i < $values.length; i++) {
        var $val = $($values[i]);
        if (removeEmpty && !$val.val() && i !== $values.length - 1) {
          $val.remove();
        }
      }

      // Refresh $values as removing elements doesn't change the stored value
      $values = self.$values.children('.value');
      if ($values.length === 0 || $values.last().val()) {
        addValue(null);
      }

      // One more refresh
      $values = self.$values.children('.value');

      $values.off('blur').off('keyup');
      $values.on({
        blur: onBlur,
        keyup: onKeyPress
      });
    };

    var onKeyPress = function() {
      handleFields(false);
    };
    var onBlur = function() {
      handleFields(true);
    };

    var addValue = function(initialValue) {
      self.$values.append(
        $('<input>').addClass('value').val(initialValue)
      );
    };

    // Open a popup asking them to dump CSV data for setting options
    var openCsvPopup = function(cb) {
      var $box = $('#modal-dialog');
      $box.empty();

      var data = self.clean() ? self.options.join(',') : null;
      var $title = $('<h1>').text('CSV data');
      var $textarea = $('<textarea>').addClass('csv-selection').val(data);
      var $submit = $('<button>').addClass('themed-button').attr('type', 'button').text(
        'Update'
      );
      var $cancel = $('<button>').addClass('themed-button').addClass('cancel').attr(
        'type', 'button'
      ).text('Cancel');

      $submit.click(function() {
        cb($textarea.val());
        $box.hide();
      });
      $cancel.click(function() {
        $box.hide();
      });

      var $controls = $('<div>').addClass('controls');
      $controls.html($submit).append($cancel);

      $box.html($title);
      $box.append($textarea);
      $box.append($controls);
      $box.show();
    };

    self.init = function(data) {
      self.renderForm();
      for (var i = 0; i < data.options.length; i++) {
        addValue(data.options[i]);
      }
      handleFields(true);

      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg oneof');
      var $values = $('<div>').addClass('value-list');
      var $csvButton = $('<button>').text('Edit as CSV').addClass('themed-button');
      $csvButton.attr('type', 'button');
      $csvButton.click(function() {
        openCsvPopup(function(data) {
          var options = data.split(/[\n,]/).map(function(e) {
            return e.replace(/^\s*(.+[^\s])\s*$/, '$1');  // trim strings
          });

          self.$values.empty();
          for (var i = 0; i < options.length; i++) {
            addValue(options[i]);
          }
          handleFields(true);
        });
      });

      $form.html($values);
      $values.before('...must be one of ');
      $form.append($csvButton);

      self.$form = $form;
      self.$values = $values;

      addValue();
      handleFields(true);

      return $form;
    };

    self.clean = function() {
      var options = [];
      self.$values.children('.value').each(function() {
        var v = $(this).val();

        if (v) {
          options.push(v);
        }
      });
      self.options = options.sort();

      if (self.options.length === 0) {
        self.error = 'No options specified for "one of" field';
        return false;
      }

      return true;
    };

    self.buildJSON = function() {
      return {condition_type: 'string:oneof', options: self.options};
    };
  };

  Widgets.ip_range = function() {
    var self = this;
    self.range = null;

    self.init = function(data) {
      self.renderForm();
      self.$form.children('[name="range"]').val(data.range);
      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg iprange');
      var $range = $('<input>').attr('name', 'range');
      $form.html($range);

      // Labelling
      $range.before('...must be in IPv4 range ');

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      self.range = self.$form.children('[name="range"]').val();
      // TODO: Validate this with a regex
      return true;
    };
    self.buildJSON = function() {
      return {condition_type: 'networking:iprange', range: self.range};
    };
  };

  Widgets.proportion = function() {
    var self = this;
    self.prop = null;

    self.init = function(data) {
      self.renderForm();
      self.$form.children('[name="prop"]').val(data.proportion * 100);
      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg proportion');
      var $prop = $('<input>').attr('name', 'prop');
      $form.html($prop);

      $prop.before('...on for ');
      $prop.after('% of values');

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      var prop = self.$form.children('[name="prop"]').val();

      // Parse float still works with junk at the end of the string, which I don't like
      if (!prop.match(/^[0-9]*\.?[0-9]+$/)) {
        self.error = 'Non-numeric proportion given';
        return false;
      }

      self.prop = parseFloat(prop / 100);
      return true;
    };

    self.buildJSON = function() {
      return {condition_type: 'proportion', proportion: self.prop};
    };
  };

  /**
   * This widget is special in that any condition type which doesn't have a defined widget
   * will use this. It's just a text area containing the raw JSON.
   */
  Widgets.raw = function() {
    var self = this;
    self.data = {condition_type: "..."};

    self.init = function(data) {
      self.data = data;
      self.initial = data;
      self.renderForm();
      return self.$form;
    };

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg raw-json');
      var $data = $('<textarea>').attr('name', 'data');
      $data.val(JSON.stringify(self.data, null, 2));

      $form.html($data);
      $data.before('...raw or unknown condition config');

      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      try {
        self.data = JSON.parse(self.$form.children('[name="data"]').val());
      } catch(err) {
        self.error = 'Unparseable JSON';
        return false;
      }

      return true;
    };

    self.buildJSON = function() {
      return self.data;
    };

  };

  // Exports
  var Conditions = {};
  Conditions.widgets = Widgets;
  Conditions.baseConditions = baseConditions;
  Conditions.subConditions = subConditions;
  Conditions.conditionTypes = conditionTypes;
  Conditions.createCondition = createCondition;

  window.Conditions = Conditions;
})(jQuery);
