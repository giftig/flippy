(function($) {
  var DEFAULT_CONDITION = {condition_type: 'False'};
  var $errorBox;

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

    self.render = function(details) {
      // TODO: This needs to be aware of various types of switches and allow
      // modifying it with a nice GUI. For now, we'll edit raw JSON instead
      var $controls = $('<div>').addClass('switch').attr('data-name', self.name);
      $controls.append($('<span>').addClass('name').text(self.name));
      $controls.append(
        $('<textarea>').addClass('config').text(
          JSON.stringify(self.condition, null, '  ')
        )
      );
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
      return $controls;
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

    self.$errorBox = cfg.error_box || $('#flippy-error');
    self.$errorBox.click(function() {
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

    // TODO: Make this nicer
    self.displayError = function(msg, title) {
      self.$errorBox.empty();

      if (title) {
        self.$errorBox.html($('<h1>').text(title));
      }
      self.$errorBox.append($('<span>').text(msg));
      self.$errorBox.show();
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
