CLASSES=\
	jcifs.smb.VolatileSmbFile					\
	org.joval.discovery.SessionFactory				\
	org.joval.identity.Credential					\
	org.joval.intf.identity.ICredential				\
	org.joval.intf.identity.ILocked					\
	org.joval.intf.io.IFile						\
	org.joval.intf.io.IFilesystem					\
	org.joval.intf.io.IRandomAccess					\
	org.joval.intf.system.IEnvironment				\
	org.joval.intf.system.IProcess					\
	org.joval.intf.system.ISession					\
	org.joval.intf.unix.io.IUnixFile				\
	org.joval.intf.unix.system.IUnixSession				\
	org.joval.intf.util.IPathRedirector				\
	org.joval.intf.util.tree.IForest				\
	org.joval.intf.util.tree.INode					\
	org.joval.intf.util.tree.ITree					\
	org.joval.intf.util.tree.ITreeBuilder				\
	org.joval.intf.windows.identity.IACE				\
	org.joval.intf.windows.identity.IDirectory			\
	org.joval.intf.windows.identity.IGroup				\
	org.joval.intf.windows.identity.IPrincipal			\
	org.joval.intf.windows.identity.IUser				\
	org.joval.intf.windows.io.IWindowsFile				\
	org.joval.intf.windows.registry.IKey				\
	org.joval.intf.windows.registry.IRegistry			\
	org.joval.intf.windows.registry.IBinaryValue			\
	org.joval.intf.windows.registry.IDwordValue			\
	org.joval.intf.windows.registry.IExpandStringValue		\
	org.joval.intf.windows.registry.IMultiStringValue		\
	org.joval.intf.windows.registry.IStringValue			\
	org.joval.intf.windows.registry.IValue				\
	org.joval.intf.windows.system.IWindowsSession			\
	org.joval.intf.windows.wmi.ISWbemObject				\
	org.joval.intf.windows.wmi.ISWbemObjectSet			\
	org.joval.intf.windows.wmi.ISWbemProperty			\
	org.joval.intf.windows.wmi.ISWbemPropertySet			\
	org.joval.intf.windows.wmi.IWmiProvider				\
	org.joval.io.BaseFile						\
	org.joval.io.LittleEndian					\
	org.joval.io.StreamLogger					\
	org.joval.io.StreamTool						\
	org.joval.io.TailDashF						\
	org.joval.os.embedded.IosNetworkInterface			\
	org.joval.os.embedded.IosSystemInfo				\
	org.joval.os.embedded.system.IosSession				\
	org.joval.os.unix.NetworkInterface				\
	org.joval.os.unix.UnixSystemInfo				\
	org.joval.os.unix.io.UnixFile					\
	org.joval.os.unix.system.Environment				\
	org.joval.os.unix.remote.system.Sudo				\
	org.joval.os.unix.remote.system.UnixSession			\
	org.joval.os.windows.WindowsSystemInfo				\
	org.joval.os.windows.identity.ActiveDirectory			\
	org.joval.os.windows.identity.Directory				\
	org.joval.os.windows.identity.Group				\
	org.joval.os.windows.identity.LocalDirectory			\
	org.joval.os.windows.identity.Principal				\
	org.joval.os.windows.identity.User				\
	org.joval.os.windows.identity.WindowsCredential			\
	org.joval.os.windows.io.WindowsFile				\
	org.joval.os.windows.io.WOW3264FilesystemRedirector		\
	org.joval.os.windows.pe.Characteristics				\
	org.joval.os.windows.pe.DLLCharacteristics			\
	org.joval.os.windows.pe.ImageDOSHeader				\
	org.joval.os.windows.pe.ImageFileHeader				\
	org.joval.os.windows.pe.ImageNTHeaders				\
	org.joval.os.windows.pe.ImageDataDirectory			\
	org.joval.os.windows.pe.ImageOptionalHeader			\
	org.joval.os.windows.pe.ImageOptionalHeader32			\
	org.joval.os.windows.pe.ImageOptionalHeader64			\
	org.joval.os.windows.pe.ImageSectionHeader			\
	org.joval.os.windows.pe.LanguageConstants			\
	org.joval.os.windows.pe.resource.ImageResourceDataEntry		\
	org.joval.os.windows.pe.resource.ImageResourceDirectory		\
	org.joval.os.windows.pe.resource.ImageResourceDirectoryEntry	\
	org.joval.os.windows.pe.resource.Types				\
	org.joval.os.windows.pe.resource.version.StringFileInfo		\
	org.joval.os.windows.pe.resource.version.StringStructure	\
	org.joval.os.windows.pe.resource.version.StringTable		\
	org.joval.os.windows.pe.resource.version.Var			\
	org.joval.os.windows.pe.resource.version.VarFileInfo		\
	org.joval.os.windows.pe.resource.version.VsFixedFileInfo	\
	org.joval.os.windows.pe.resource.version.VsVersionInfo		\
	org.joval.os.windows.registry.BaseRegistry			\
	org.joval.os.windows.registry.BinaryValue			\
	org.joval.os.windows.registry.DwordValue			\
	org.joval.os.windows.registry.ExpandStringValue			\
	org.joval.os.windows.registry.MultiStringValue			\
	org.joval.os.windows.registry.StringValue			\
	org.joval.os.windows.registry.Value				\
	org.joval.os.windows.registry.WOW3264RegistryRedirector		\
	org.joval.os.windows.remote.identity.SmbACE			\
	org.joval.os.windows.remote.io.SmbFilesystem			\
	org.joval.os.windows.remote.io.SmbFileProxy			\
	org.joval.os.windows.remote.io.SmbRandomAccessProxy		\
	org.joval.os.windows.remote.registry.Key			\
	org.joval.os.windows.remote.registry.Registry			\
	org.joval.os.windows.remote.system.WindowsProcess		\
	org.joval.os.windows.remote.system.WindowsSession		\
	org.joval.os.windows.remote.wmi.WmiConnection			\
	org.joval.os.windows.remote.wmi.process.ICreateFlags		\
	org.joval.os.windows.remote.wmi.process.IShowWindow		\
	org.joval.os.windows.remote.wmi.process.SWbemSecurity		\
	org.joval.os.windows.remote.wmi.process.Win32Process		\
	org.joval.os.windows.remote.wmi.process.Win32ProcessStartup	\
	org.joval.os.windows.remote.wmi.query.SimpleSWbemObject		\
	org.joval.os.windows.remote.wmi.query.SimpleSWbemObjectSet	\
	org.joval.os.windows.remote.wmi.query.SimpleSWbemProperty	\
	org.joval.os.windows.remote.wmi.query.SimpleSWbemPropertySet	\
	org.joval.os.windows.system.Environment				\
	org.joval.os.windows.wmi.WmiException				\
	org.joval.plugin.OfflinePlugin					\
	org.joval.plugin.OnlinePlugin					\
	org.joval.plugin.RemotePlugin					\
	org.joval.plugin.adapter.cisco.ios.LineAdapter			\
	org.joval.plugin.adapter.cisco.ios.Version55Adapter		\
	org.joval.plugin.adapter.cisco.ios.VersionAdapter		\
	org.joval.plugin.adapter.independent.BaseFileAdapter		\
	org.joval.plugin.adapter.independent.Environmentvariable58Adapter	\
	org.joval.plugin.adapter.independent.EnvironmentvariableAdapter	\
	org.joval.plugin.adapter.independent.FamilyAdapter		\
	org.joval.plugin.adapter.independent.TextfilecontentAdapter	\
	org.joval.plugin.adapter.independent.Textfilecontent54Adapter	\
	org.joval.plugin.adapter.independent.VariableAdapter		\
	org.joval.plugin.adapter.independent.XmlfilecontentAdapter	\
	org.joval.plugin.adapter.linux.RpminfoAdapter			\
	org.joval.plugin.adapter.macos.PlistAdapter			\
	org.joval.plugin.adapter.solaris.IsainfoAdapter			\
	org.joval.plugin.adapter.solaris.PackageAdapter			\
	org.joval.plugin.adapter.solaris.Patch54Adapter			\
	org.joval.plugin.adapter.solaris.PatchAdapter			\
	org.joval.plugin.adapter.solaris.SmfAdapter			\
	org.joval.plugin.adapter.unix.FileAdapter			\
	org.joval.plugin.adapter.unix.ProcessAdapter			\
	org.joval.plugin.adapter.unix.RunlevelAdapter			\
	org.joval.plugin.adapter.unix.UnameAdapter			\
	org.joval.plugin.adapter.windows.FileAdapter			\
	org.joval.plugin.adapter.windows.Fileeffectiverights53Adapter	\
	org.joval.plugin.adapter.windows.GroupAdapter			\
	org.joval.plugin.adapter.windows.GroupSidAdapter		\
	org.joval.plugin.adapter.windows.RegistryAdapter		\
	org.joval.plugin.adapter.windows.SidAdapter			\
	org.joval.plugin.adapter.windows.SidSidAdapter			\
	org.joval.plugin.adapter.windows.UserAdapter			\
	org.joval.plugin.adapter.windows.UserSid55Adapter		\
	org.joval.plugin.adapter.windows.UserSidAdapter			\
	org.joval.plugin.adapter.windows.Wmi57Adapter			\
	org.joval.plugin.adapter.windows.WmiAdapter			\
	org.joval.ssh.identity.SshCredential				\
	org.joval.ssh.io.SftpError					\
	org.joval.ssh.io.SftpFile					\
	org.joval.ssh.io.SftpFilesystem					\
	org.joval.ssh.system.SshProcess					\
	org.joval.ssh.system.SshSession					\
	org.joval.test.AD						\
	org.joval.test.Exec						\
	org.joval.test.FS						\
	org.joval.test.PE						\
	org.joval.test.Reg						\
	org.joval.test.Remote						\
	org.joval.test.WMI						\
	org.joval.util.Base64						\
	org.joval.util.BaseSession					\
	org.joval.util.JSchLogger					\
	org.joval.util.tree.CachingTree					\
	org.joval.util.tree.PropertyHierarchy				\
	org.joval.util.tree.Forest					\
	org.joval.util.tree.Node					\
	org.joval.util.tree.Tree
