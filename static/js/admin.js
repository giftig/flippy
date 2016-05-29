(function($) {
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

      var $cancelButton = $('<button>')
        .text('Cancel')
        .addClass('cancel')
        .attr('data-action', 'cancel')
        .click(function() {
          var $conf = $(this).parents('.switch').find('.config');
          $conf.val($conf.text());
        });

      $persistControls.append($saveButton).append($cancelButton);
      $controls.append($persistControls);
      return $controls;
    };

    self.updateSwitch = function(switchContent, success, err) {
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
        complete: success,
        error: function(resp) {
          self.displayError(resp.responseText, resp.status + ' ' + resp.statusText);
          err();
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

    self.listSwitches = function() {
      $.ajax({
        url: self.baseUrl + 'switch/',
        type: 'GET',
        success: function(data) {
          self.renderSwitches(data);
        },
        error: function(data) {
          self.displayError(data);
        }
      });
    };

    self.renderSwitches = function(switches) {
      self.$container.empty();
      for (var i = 0; i < switches.length; i++) {
        self.$container.append(new SwitchBuilder(switches[i], self).render());
      }
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
  };

  window.FlippyAdmin = Admin;
})(jQuery);
