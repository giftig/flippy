(function($) {
  var DEFAULT_CONDITION = {condition_type: 'false'};
  var $errorBox;

  var ConditionWidgets = {};

  // Pretty names. Default to the same name if not present in this mapping
  var conditionAliases = {
    one_of: 'one of',
    ip_range: 'IPv4 range',
    multiple: 'and/or',
    namespaced: 'field must match...',
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

  var createCondition = function(name) {
    var t = conditionTypes[name] || name;
    var Condition = ConditionWidgets[t];
    return Condition ? new Condition() : null;
  };

  var baseConditions = ['namespaced', 'multiple', 'not', 'on', 'off'];
  var subConditions = [
    'equals',
    'namespaced',
    'multiple',
    'not',
    'regex',
    'substring',
    'one_of',
    'ip_range'
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
  ConditionWidgets.namespaced = function() {
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

        var sub = new ConditionWidgets[condName]();
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

      // Labelling
      $field.before('The field ');
      $field.after(' ... ');
      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      self.field = self.$form.children('[name="field"]').val();
      self.fallback = self.$form.children('[name="fallback"]').val() === 'true';

      return !!self.condition && self.condition.clean();
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

  ConditionWidgets.not = function() {
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

        var sub = new ConditionWidgets[condName]();
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
      return !!self.condition && self.condition.clean();
    };

    self.buildJSON = function() {
      return {
        condition_type: 'not',
        condition: self.condition.buildJSON()
      };
    };
  };

  ConditionWidgets.equals = function() {
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

  ConditionWidgets.on = function() {
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
  ConditionWidgets.off = function() {
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
  ConditionWidgets.multiple = function() {
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
        var cond = new ConditionWidgets[condType]();
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
        return false;
      }
      self.conditionType = self.$form.children('[name="type"]').val();

      for (var i = 0; i < self.conditions.length; i++) {
        if (!self.conditions[i].clean()) {
          return false;
        }
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

  ConditionWidgets.substring = function() {
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
  ConditionWidgets.regex = function() {
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

  ConditionWidgets.one_of = function() {
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

      $form.html($values);
      $values.before('...must be one of ');

      self.$form = $form;
      self.$values = $values;

      addValue();
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

      return self.options.length !== 0;
    };

    self.buildJSON = function() {
      return {condition_type: 'string:oneof', options: self.options};
    };
  };

  ConditionWidgets.ip_range = function() {
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
      return true;
    };
    self.buildJSON = function() {
      return {condition_type: 'networking:iprange', range: self.range};
    };
  };

  // Controls for building a switch
  var SwitchBuilder = function(initial, admin) {
    var self = this;
    var MODE_GUI = 0, MODE_ADVANCED = 1;

    initial = initial || {};
    self.name = initial.name || 'New switch';
    self.condition = initial.condition || {switch_type: 'false'};
    self.initialCondition = self.condition;
    self.admin = admin;
    self.mode = MODE_GUI;

    self.baseUrl = self.admin.baseUrl;
    self.displayError = self.admin.displayError;
    self.displaySuccess = self.admin.displaySuccess;

    self.save = function() {
      var $conf = self.$controls.find('.config');
      var $save = $conf.find('[data-action="save"]');
      $save.attr('disabled', true);

      // Update the JSON in the textarea from the GUI components
      if (self.mode === MODE_GUI && self.widget) {
        if (!self.widget.clean()) {
          // TODO: Report an error here
          return;
        }
        $conf.text(JSON.stringify(self.widget.buildJSON(), null, 2));
      }

      var data = $conf.val();

      var success = function() {
        $save.attr('disabled', false);
        $conf.text(data);
        $conf.val(data);

        self.displaySuccess(
          'Switch <span class="switch-name">' + self.name + '</span> successfully updated.',
          'Switch saved'
        );
      };
      var err = function() {
        $save.attr('disabled', false);
      };
      self.updateSwitch(data, success, err);
    };

    self.render = function() {
      // TODO: This needs to be aware of various types of switches and allow
      // modifying it with a nice GUI. For now, we'll edit raw JSON instead
      var $controls = $('<div>').addClass('switch').attr('data-name', self.name);
      $controls.append($('<span>').addClass('name').text(self.name));
      var $underlyingData = $('<textarea>').addClass('config').text(
        JSON.stringify(self.condition, null, '  ')
      );
      $controls.append($underlyingData);
      $controls.append(self.renderGui(self.condition, $underlyingData));

      var $persistControls = $('<div>').addClass('persist');
      var $saveButton = $('<button>')
        .text('Save')
        .addClass('save')
        .attr('data-action', 'save')
        .click(self.save);

      var $cancelButton = $('<button>').text('Cancel').addClass('cancel');
      $cancelButton.click(function() {
        var $conf = $(this).parents('.switch').find('.config');
        $conf.val($conf.text());
        $controls.find('.switch-builder').replaceWith(
          self.renderGui(self.condition, $underlyingData)
        );
      });

      var $deleteButton = $('<button>').text('Delete').addClass('delete');
      $deleteButton.click(function() {
        if(!confirm('Are you sure you want to delete the ' + self.name + ' switch?')) {
          return;
        }

        self.deleteSwitch();
      });

      $persistControls.append($saveButton).append($cancelButton).append($deleteButton);
      $controls.append($persistControls);

      self.$controls = $controls;
      return self.$controls;
    };

    self.renderGui = function(initial, $underlying) {
      var $builder = $('<div>').addClass('switch-builder');
      var $select = baseConditions.asOptions().addClass('main-condition-selector');
      var $stage = $('<div>').addClass('stage');

      var renderWidget = function(type, data) {
        if (!type) {
          return;
        }

        self.widget = new ConditionWidgets[type]();
        var $condForm = data ? self.widget.init(data) : self.widget.renderForm();
        $stage.html($condForm);
      };

      $select.on('change', function() {
        renderWidget($(this).val());
      });

      renderWidget(conditionTypes[initial.condition_type] || initial.condition_type, initial);
      $builder.html($select).append($stage);
      return $builder;
    };

    self.updateSwitch = function(switchContent, success, err) {
      success = success || function() {};
      err = err || function() {};

      try {
        self.condition = JSON.parse(switchContent);
      } catch(e) {
        self.displayError(e, 'Bad JSON Data');
        err();
        return;
      }

      $.ajax({
        url: self.baseUrl + 'switch/' + self.name + '/',
        type: 'POST',
        data: JSON.stringify(self.condition),
        dataType: 'json',
        contentType: 'application/json',
        success: success,
        error: function(resp) {
          self.displayError(resp.responseText, resp.status + ' ' + resp.statusText);
          err();
        }
      });
    };

    self.deleteSwitch = function() {
      $.ajax({
        url: self.baseUrl + 'switch/' + self.name + '/',
        type: 'DELETE',
        success: function() {
          self.admin.switchDeleted(self.name);
        },
        error: function(resp) {
          self.displayError(resp.responseText, resp.status + ' ' + resp.statusText);
        }
      });
    };

  };

  var Admin = function(cfg) {
    var self = this;
    self.protocol = cfg.protocol;
    self.host = cfg.host;
    self.port = cfg.port;
    self.pathPrefix = cfg.path_prefix || '/';

    self.$container = cfg.container;

    if (!self.protocol && !self.host && !self.port) {
      self.baseUrl = self.pathPrefix;
    } else {
      self.baseUrl = (
        (self.protocol || 'http') + '://' +
        (self.host || 'localhost') + ':' + (self.port || 80) + '/' +
        self.pathPrefix
      );
    }

    self.$msgBox = cfg.message_box || $('#flippy-admin-message');
    self.$msgBox.addClass('flippy-admin-message');
    self.$msgBox.click(function() {
      $(this).hide();
    });

    self.listSwitches = function(offset, cb) {
      offset = offset || 0;

      $.ajax({
        url: self.baseUrl + 'switch/?offset=' + offset,
        type: 'GET',
        success: function(data) {
          self.renderSwitches(data);
        },
        error: function(data) {
          self.displayError(data);
        },
        complete: cb
      });
    };

    self.renderSwitches = function(switches) {
      for (var i = 0; i < switches.length; i++) {
        self.$switchList.append(new SwitchBuilder(switches[i], self).render());
      }
    };

    // Render the controls for new switches etc.
    self.renderControls = function() {
      // HEADER CONTROLS
      var $newSwitch = $('<button>').addClass('new-switch').text('Create switch');
      var $newSwitchControls = $('<span>').addClass('new-switch-controls').addClass('extended');
      var $nameBox = $('<input>').addClass('name').val('new_switch');
      var $confirmButton = $('<button>').addClass('confirm').text('Create');
      $newSwitchControls.html($nameBox).append($confirmButton);

      $newSwitch.click(function() {
        $newSwitchControls.show();
        $newSwitch.hide();
      });
      $confirmButton.click(function() {
        var name = $nameBox.val();
        $nameBox.val('new_switch');

        if (self.$container.find('.switch[data-name="' + name + '"]').length !== 0) {
          self.displayError('A switch with that name already exists.', 'Bad switch name');
          return;
        }

        var newSwitch = new SwitchBuilder(
          {name: name, condition: DEFAULT_CONDITION}, self
        );
        self.$switchList.prepend(newSwitch.render());
        $newSwitchControls.hide();
        $newSwitch.show();
      });
      self.$headerControls.html($newSwitch);
      self.$headerControls.append($newSwitchControls);

      // FOOTER CONTROLS
      $showMore = $('<button>').addClass('show-more').text('Show more...');
      $showMore.click(function() {
        $showMore.attr('disabled', true);
        var offset = self.$container.find('.switch').length;

        self.listSwitches(offset, function() {
          $showMore.attr('disabled', false);
        });
      });
      self.$footerControls.html($showMore);

    };

    self.displayMessage = function(msg, title, type) {
      self.$msgBox.hide();
      self.$msgBox.empty();

      if (title) {
        self.$msgBox.html($('<h1>').text(title));
      }
      self.$msgBox.append($('<span>').html(msg));

      self.$msgBox[type === 'error' ? 'addClass' : 'removeClass']('error');
      self.$msgBox.show();
    };
    self.displaySuccess = self.displayInfo = function(msg, title) {
      self.displayMessage(msg, title, 'info');
    };
    self.displayError = function(msg, title) {
      self.displayMessage(msg, title, 'error');
    };

    self.render = function() {
      self.$container.empty();
      self.$headerControls = $('<div>').addClass('controls');
      self.$switchList = $('<div>').addClass('switches');
      self.$footerControls = $('<div>').addClass('controls');

      self.$container.append(self.$headerControls);
      self.$container.append(self.$switchList)
      self.$container.append(self.$footerControls);

      self.renderControls();
      self.listSwitches();
    };

    // Called to indicate a switch has been removed
    self.switchDeleted = function(name) {
      self.$container.find('.switch[data-name="' + name + '"]').remove();
    };
  };

  window.FlippyAdmin = Admin;
})(jQuery);
