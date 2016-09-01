(function($) {
  var DEFAULT_CONDITION = {condition_type: 'false'};
  var $errorBox;

  var ConditionWidgets = {};
  var baseConditions = ['namespaced', 'multiple'];
  var subConditions = ['equals', 'namespaced', 'multiple'];

  subConditions.asOptions = function(defaultLabel) {
    defaultLabel = defaultLabel || 'Select condition';

    var $conditions = $('<select>');
    $conditions.append($('<option>').val('').text(defaultLabel));

    for (var i = 0; i < subConditions.length; i++) {
      var cond = subConditions[i];
      var $opt = $('<option>').text(cond).val(cond);
      $conditions.append($opt);
    }

    return $conditions;
  };

  /*
   * All widgets must define:
   *
   * renderForm:  render a form to help configure the widget
   * buildJSON:   build javascript objects representing the JSON structure
   * clean:       pull in completed form data and verify it; return whether it's valid
   */
  ConditionWidgets.namespaced = function() {
    var self = this;

    self.field = null;
    self.condition = null;

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg namespaced');
      var $field = $('<input>').attr('name', 'field');
      var $fallback = $('<select>').attr('name', 'fallback').append(
        $('<option>').val('').text('Select fallback'),
        $('<option>').val('true').text('true'),
        $('<option>').val('false').text('false')
      );

      var $conditions = subConditions.asOptions('Select condition').attr('name', 'condition');

      $conditions.on('change', function() {
        var condName = $(this).val();
        var $oldSubform = $form.find('.subcondition');

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
        $oldSubform.html($subform);
      });

      $form.html($field).append($conditions);

      // Labelling
      $field.before('The field ');
      $field.after(' ... ');
      self.$form = $form;
      return $form;
    };

    self.clean = function() {
      self.field = self.$form.find('[name="field"]').val();

      if (!self.condition.clean()) {
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

  ConditionWidgets.equals = function() {
    var self = this;

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
      var value = self.$form.find('[name="value"]').val();

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

  // For and/or condition lists
  ConditionWidgets.multiple = function() {
    var self = this;
    self.conditions = [];
    self.conditionType = null;
    var nextIndex = 0;

    self.renderForm = function() {
      var $form = $('<form>').addClass('condition-cfg multi');
      var $type = $('<select>').attr('name', 'type').append(
        $('<option>').text('all').val('and'),
        $('<option>').text('one').val('or')
      );
      var $stagingArea = $('<div>');
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
      return $form;
    };

    self.clean = function() {
      if (self.conditions.length === 0) {
        return false;
      }
      self.conditionType = self.$form.find('[name="type"]').val();

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

  // Controls for building a switch
  var SwitchBuilder = function(initial, admin) {
    var self = this;
    initial = initial || {};
    self.name = initial.name || 'New switch';
    self.condition = initial.condition || {switch_type: 'false'};
    self.initialCondition = self.condition;
    self.admin = admin;

    self.baseUrl = self.admin.baseUrl;
    self.displayError = self.admin.displayError;
    self.displaySuccess = self.admin.displaySuccess;

    self.render = function(details) {
      // TODO: This needs to be aware of various types of switches and allow
      // modifying it with a nice GUI. For now, we'll edit raw JSON instead
      var $controls = $('<div>').addClass('switch').attr('data-name', self.name);
      $controls.append($('<span>').addClass('name').text(self.name));
      var $underlyingData = $('<textarea>').addClass('config').text(
        JSON.stringify(self.condition, null, '  ')
      );
      $controls.append($underlyingData);
      $controls.append(self.renderGui($underlyingData));

      var $persistControls = $('<div>').addClass('persist');
      var $saveButton = $('<button>')
        .text('Save')
        .addClass('save')
        .attr('data-action', 'save')
        .click(function() {
          var $save = $(this);
          var $conf = $save.parents('.switch').find('.config');
          $save.attr('disabled', true);
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
        });

      var $cancelButton = $('<button>').text('Cancel').addClass('cancel');
      $cancelButton.click(function() {
        var $conf = $(this).parents('.switch').find('.config');
        $conf.val($conf.text());
        $controls.find('.switch-builder').replaceWith(self.renderGui());
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

    self.renderGui = function($underlying) {
      var $builder = $('<div>').addClass('switch-builder');
      var $select = $('<select>').addClass('main-condition-selector');
      $select.append($('<option>').val('').text('Choose one'));

      for (var i = 0; i < baseConditions.length; i++) {
        var cond = baseConditions[i];
        var $opt = $('<option>').val(cond).text(cond);
        $select.append($opt);
      }

      $select.on('change', function() {
        var condValue = $(this).val();
        if (!condValue) {
          return;
        }

        var cond = new ConditionWidgets[condValue]();
        var $condForm = cond.renderForm();
        var $submit = $('<input>').attr('type', 'submit').addClass('save').val('Update JSON');
        $condForm.append($submit);
        $condForm.on('submit', function(e) {
          e.preventDefault();

          if (!cond.clean()) {
            return false;
          }
          $underlying.text(JSON.stringify(cond.buildJSON(), null, 2));
          return false;
        });
        $builder.html($condForm);
      });
      $builder.html($select);
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
      var $newSwitch = $('<button>').addClass('new-switch').text('New switch');
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
