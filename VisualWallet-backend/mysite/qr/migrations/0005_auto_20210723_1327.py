# Generated by Django 3.2.5 on 2021-07-23 05:27

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('qr', '0004_auto_20210515_1315'),
    ]

    operations = [
        migrations.AddField(
            model_name='skinfo',
            name='date',
            field=models.CharField(default='null', max_length=100),
            preserve_default=False,
        ),
        migrations.AlterField(
            model_name='skinfo',
            name='id',
            field=models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID'),
        ),
    ]
